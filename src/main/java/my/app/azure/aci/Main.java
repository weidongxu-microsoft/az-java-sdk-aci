package my.app.azure.aci;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.core.util.logging.ClientLogger;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.resources.ResourceManager;

public class Main {

    private static final ClientLogger LOGGER = new ClientLogger(Main.class);

    public static void main(String ...args) {

        // see https://github.com/Azure/azure-sdk-for-java/tree/main/sdk/resourcemanager#authentication for environment variables

        // define env variable AZURE_SUBSCRIPTION_ID for subscription ID

        // DO NOT CODE SECRET
        TokenCredential credential = new DefaultAzureCredentialBuilder().build();
        AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);

        final ResourceManager resourceManager = ResourceManager.configure()
                .withLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS)
                .authenticate(credential, profile)
                .withDefaultSubscription();

    }
}
