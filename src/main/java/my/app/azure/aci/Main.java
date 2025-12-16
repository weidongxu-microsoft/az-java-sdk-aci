package my.app.azure.aci;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.Region;
import com.azure.core.management.polling.PollResult;
import com.azure.core.management.profile.AzureProfile;
import com.azure.core.util.logging.ClientLogger;
import com.azure.core.util.polling.LongRunningOperationStatus;
import com.azure.core.util.polling.PollerFlux;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.network.models.Network;
import com.azure.resourcemanager.network.models.Subnet;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {

    private static final ClientLogger LOGGER = new ClientLogger(Main.class);

    private static final String RG_NAME = "rg-weidxu-aci";
    private static final Region REGION = Region.US_WEST3;

    public static void main(String ...args) {

        // see https://github.com/Azure/azure-sdk-for-java/tree/main/sdk/resourcemanager#authentication for environment variables

        // define env variable AZURE_SUBSCRIPTION_ID for subscription ID

        // DO NOT CODE SECRET
        TokenCredential credential = new DefaultAzureCredentialBuilder().build();
        AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);

        final AzureResourceManager azure = AzureResourceManager.configure()
                .withLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS)
                .authenticate(credential, profile)
                .withDefaultSubscription();

        List<String> resourceNames = IntStream.range(0, 10)
                .mapToObj(n -> "aci-weidxu-name" + n)
                .collect(Collectors.toList());

        createInstances(azure, resourceNames);

        stopInstances(azure, resourceNames);

        startInstancesWithLog(azure, resourceNames);

        deleteInstances(azure, resourceNames);
    }

    private static void createInstances(AzureResourceManager azure, List<String> resourceNames) {
        // create resource group
        azure.resourceGroups().define(RG_NAME)
                .withRegion(REGION)
                .create();

        // create virtual network
        Network network = azure.networks().define("vnet-weidxu-aci")
                .withRegion(REGION)
                .withExistingResourceGroup(RG_NAME)
                .withAddressSpace("10.0.0.0/24")
                .defineSubnet("default")
                .withAddressPrefix("10.0.0.0/24")
                .withDelegation("Microsoft.ContainerInstance/containerGroups")
                .attach()
                .create();
        Subnet subnet = network.subnets().get("default");

        Flux.fromStream(resourceNames.stream()
                .map(name -> azure.containerGroups()
                        .define(name)
                        .withRegion(REGION)
                        .withExistingResourceGroup(RG_NAME)
                        .withLinux()
                        .withPublicImageRegistryOnly()
                        .withoutVolume()
                        .withContainerInstance("nginx", 80)
                        .withExistingSubnet(subnet)
                        .createAsync()))
                .flatMap(r -> r)
                .blockLast();
    }

    private static void deleteInstances(AzureResourceManager azure, List<String> resourceNames) {
        azure.resourceGroups().deleteByName(RG_NAME);
    }

    private static void stopInstances(AzureResourceManager azure, List<String> resourceNames) {
        Flux.fromStream(resourceNames.stream()
                .map(name -> azure.containerGroups().manager().serviceClient().getContainerGroups()
                        .stopAsync(RG_NAME, name)))
                .flatMap(r -> r)
                .blockLast();
    }

    private static void startInstancesWithLog(AzureResourceManager azure, List<String> resourceNames) {
        ConcurrentMap<String, Long> provisionStartTimestamps = new ConcurrentHashMap<>();
        ConcurrentMap<String, Long> provisionCompleteTimestamps = new ConcurrentHashMap<>();
        ConcurrentMap<String, String> provisionStates = new ConcurrentHashMap<>();

        Flux.fromStream(resourceNames.stream()
                .map(name -> {
                    PollerFlux<PollResult<Void>, Void> pollerFlux = azure.containerGroups().manager().serviceClient().getContainerGroups()
                            .beginStartAsync(RG_NAME, name);
                    // shorten the default polling interval to 5 seconds
                    pollerFlux.setPollInterval(Duration.ofSeconds(5));
                    return pollerFlux.last().flatMap(pollResult -> {
                        if (pollResult.getStatus() != LongRunningOperationStatus.SUCCESSFULLY_COMPLETED) {
                            return Mono.error(new RuntimeException("Failed to start container group"));
                        } else {
                            return Mono.empty();
                        }
                    }).doOnSubscribe(ignored -> {
                        // log start timestamp
                        provisionStartTimestamps.put(name, System.nanoTime());
                    }).doOnTerminate(() -> {
                        // log complete timestamp
                        provisionCompleteTimestamps.put(name, System.nanoTime());
                    });
                }))
                .flatMap(r -> r)
                .blockLast();

        // check provision state
        Flux.fromStream(resourceNames.stream()
                .map(name -> azure.containerGroups()
                        .getByResourceGroupAsync(RG_NAME, name)
                        .doOnSuccess(r -> provisionStates.put(name, r.provisioningState()))))
                .flatMap(r -> r)
                .blockLast();

        // log provision time and state
        resourceNames.forEach(name -> {
            long start = provisionStartTimestamps.get(name);
            long complete = provisionCompleteTimestamps.get(name);
            String state = provisionStates.get(name);
            long durationInSeconds = (complete - start) / 1_000_000_000;
            LOGGER.info("Container Group: {}, Provisioning State: {}, Time Taken (seconds): {}", name, state, durationInSeconds);
        });
    }
}
