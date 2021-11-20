# kuta-akka-samples

Commercial projects usually include gateway nodes, service nodes, etc. all external communication is the responsibility of the gateway node, which forwards the message to the service node after received the message. when a issue processed, the service node replies a/some message to the client.

## Get start

Quickly start a service that can be accessed using HTTP

- **1. clone** https://github.com/kutasms/kuta-akka-samples.git
- **2. open with eclipse** workspace: your-git-clone-dir/kuta-akka-samples
- **3. edit** gateway/src/main/resources/application.conf
- **4. update** remote.artery.hostname、remote.artery.port and cluster.seed-nodes
- **5. maven update**
- **6. maven build**
- **7. start** GatewayApp.java as Java Application
- **8. test** use Postman to test http://localhost:8081/RESTful
