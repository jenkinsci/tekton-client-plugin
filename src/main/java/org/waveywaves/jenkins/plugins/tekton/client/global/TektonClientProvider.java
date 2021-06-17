package org.waveywaves.jenkins.plugins.tekton.client.global;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.tekton.client.DefaultTektonClient;
import io.fabric8.tekton.client.TektonClient;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


// Inspired from KubernetesClientProvider of kubernetes plugin
public class TektonClientProvider {
    private static final Logger LOGGER = Logger.getLogger(TektonClientProvider.class.getName());
    /**
     * Client expiration in seconds.
     *
     * Some providers such as Amazon EKS use a token with 15 minutes expiration, so expire clients after 10 minutes.
     */
    private static final long CACHE_EXPIRATION = Long.getLong(
            TektonClientProvider.class.getPackage().getName() + ".clients.cacheExpiration", TimeUnit.MINUTES.toSeconds(10));

    private static final Cache<String, Client> clients = Caffeine.newBuilder()
            .expireAfterWrite(CACHE_EXPIRATION, TimeUnit.SECONDS)
            .removalListener( (key, value, cause) -> {
                Client client = (Client) value;
                if (client != null) {
                    LOGGER.log(Level.FINE, () -> "Expiring Kubernetes client " + key + " " + client.client + ": " + cause);
                }
            } )
            .build();

    static TektonClient createClient(TektonCloud cloud) {
        String displayName = cloud.getDisplayName();
        final Client c = clients.getIfPresent(displayName);
        if (c == null) {
            TektonClient client = new DefaultTektonClient();
            clients.put(displayName, new Client(getValidity(cloud), client));
            return client;
        }
        return c.getClient();
    }

    private static int getValidity(TektonCloud cloud) {
        Object cloudObjects[] = {
                cloud.getServerUrl(),
                cloud.getNamespace(),
                cloud.getServerCertificate(),
                //cloud.getCredentialsId(),
                cloud.isSkipTlsVerify(),
                //cloud.getConnectTimeout(),
                //cloud.getReadTimeout(),
                //cloud.getMaxRequestsPerHostStr(),
                //cloud.isUseJenkinsProxy()
        };
        return Arrays.hashCode(cloudObjects);
    }

    private static class Client {
        private final TektonClient client;
        private final int validity;

        public Client(int validity, TektonClient client) {
            this.client = client;
            this.validity = validity;
        }

        public TektonClient getClient() {
            return client;
        }

        public int getValidity() {
            return validity;
        }
    }

}
