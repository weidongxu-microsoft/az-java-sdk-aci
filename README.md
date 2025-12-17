# Azure Container Instances Java Sample

## Prerequisites

- Java 11 or later
- Maven
- An Azure subscription with permission to manage Container Instances
- Authentication configured for the Java SDK (see the [Azure SDK authentication guide](https://github.com/Azure/azure-sdk-for-java/tree/main/sdk/resourcemanager#authentication))
- `AZURE_SUBSCRIPTION_ID` in environment variables so the sample can bind to the correct subscription

## Run the sample

1. Install dependencies and compile:

	```
	mvn clean package
	```

2. Execute the entry point:

	```
	mvn exec:java
	```

The app stops each container group, sleeps for five minutes to allow Azure to settle, then restarts the fleet with a shortened poll interval so provisioning progress be updated quicker than default interval.

## Sample output

```
11:31:53.908 [c.a.c.u.l.ClientLogger] INFO  [my.app.azure.aci.Main.main()] - Wait 5 minute for 'stop' operations to settle...
11:36:53.914 [c.a.c.u.l.ClientLogger] INFO  [my.app.azure.aci.Main.main()] - Start all containers...
11:38:25.730 [c.a.c.u.l.ClientLogger] INFO  [my.app.azure.aci.Main.main()] - Container Group: aci-weidxu-name0, Provisioning State: Succeeded, Time Taken (seconds): 32
11:38:25.731 [c.a.c.u.l.ClientLogger] INFO  [my.app.azure.aci.Main.main()] - Container Group: aci-weidxu-name1, Provisioning State: Succeeded, Time Taken (seconds): 37
11:38:25.731 [c.a.c.u.l.ClientLogger] INFO  [my.app.azure.aci.Main.main()] - Container Group: aci-weidxu-name2, Provisioning State: Succeeded, Time Taken (seconds): 37
11:38:25.731 [c.a.c.u.l.ClientLogger] INFO  [my.app.azure.aci.Main.main()] - Container Group: aci-weidxu-name3, Provisioning State: Succeeded, Time Taken (seconds): 37
11:38:25.732 [c.a.c.u.l.ClientLogger] INFO  [my.app.azure.aci.Main.main()] - Container Group: aci-weidxu-name4, Provisioning State: Succeeded, Time Taken (seconds): 38
11:38:25.732 [c.a.c.u.l.ClientLogger] INFO  [my.app.azure.aci.Main.main()] - Container Group: aci-weidxu-name5, Provisioning State: Succeeded, Time Taken (seconds): 90
11:38:25.732 [c.a.c.u.l.ClientLogger] INFO  [my.app.azure.aci.Main.main()] - Container Group: aci-weidxu-name6, Provisioning State: Succeeded, Time Taken (seconds): 38
11:38:25.733 [c.a.c.u.l.ClientLogger] INFO  [my.app.azure.aci.Main.main()] - Container Group: aci-weidxu-name7, Provisioning State: Succeeded, Time Taken (seconds): 78
11:38:25.733 [c.a.c.u.l.ClientLogger] INFO  [my.app.azure.aci.Main.main()] - Container Group: aci-weidxu-name8, Provisioning State: Succeeded, Time Taken (seconds): 32
11:38:25.733 [c.a.c.u.l.ClientLogger] INFO  [my.app.azure.aci.Main.main()] - Container Group: aci-weidxu-name9, Provisioning State: Succeeded, Time Taken (seconds): 37
11:38:26.456 [c.a.c.u.l.ClientLogger] INFO  [my.app.azure.aci.Main.main()] - Container Group: aci-weidxu-name0, Log Content: /docker-entrypoint.sh: /docker-entrypoint.d/ is not empty, will attempt to perform configuration
/docker-entrypoint.sh: Looking for shell scripts in /docker-entrypoint.d/
/docker-entrypoint.sh: Launching /docker-entrypoint.d/10-listen-on-ipv6-by-default.sh
10-listen-on-ipv6-by-default.sh: info: Getting the checksum of /etc/nginx/conf.d/default.conf
10-listen-on-ipv6-by-default.sh: info: Enabled listen on IPv6 in /etc/nginx/conf.d/default.conf
/docker-entrypoint.sh: Sourcing /docker-entrypoint.d/15-local-resolvers.envsh
/docker-entrypoint.sh: Launching /docker-entrypoint.d/20-envsubst-on-templates.sh
/docker-entrypoint.sh: Launching /docker-entrypoint.d/30-tune-worker-processes.sh
/docker-entrypoint.sh: Configuration complete; ready for start up
2025/12/17 03:37:24 [notice] 18#18: using the "epoll" event method
2025/12/17 03:37:24 [notice] 18#18: nginx/1.29.4
2025/12/17 03:37:24 [notice] 18#18: built by gcc 14.2.0 (Debian 14.2.0-19)
2025/12/17 03:37:24 [notice] 18#18: OS: Linux 6.1.146.1-microsoft-standard
2025/12/17 03:37:24 [notice] 18#18: getrlimit(RLIMIT_NOFILE): 1024:1048576
2025/12/17 03:37:24 [notice] 18#18: start worker processes
2025/12/17 03:37:24 [notice] 18#18: start worker process 45
```

Each container group writes the provisioning state and duration to the log, followed by the container's stdout/stderr stream (nginx in this sample).

## Troubleshooting

- Public container registries such as Docker Hub enforce pull limits and can emit `409 RegistryErrorResponse`. Consider referencing an Azure Container Registry image for production scenarios.
- Set `AZURE_LOG_LEVEL=body` (or adjust the `HttpLogDetailLevel`) if you need to inspect the REST payloads for debugging.
