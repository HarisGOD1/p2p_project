# P2P Spoofing Chat Application

A research-oriented P2P chat application built with `jvm-libp2p` and `pcap4j`. It implements a custom transport layer that allows for raw packet manipulation and source IP/port spoofing to bypass NAT restrictions.

## Features
- **Custom Pcap Transport**: Bypasses standard Netty I/O to inject and sniff raw UDP/IP packets.
- **Source IP Spoofing**: Allows Host B to masquerade as Host P when communicating with Host A.
- **Registry Protocol**: A public-node discovery mechanism for sharing gray IP addresses.
- **Signaling Coordination**: Relay punch requests through a public node to prepare peers for spoofed connections.

## Prerequisites
- **Java 21**: The project requires JDK 21+.
- **libpcap**: The system must have `libpcap` installed (for `pcap4j`).
- **Privileges**: Running the application requires `sudo` (root) permissions to access raw network interfaces.

## Building the Project

```bash
# Set Java Home if not already set (e.g., via SDKMAN)
export JAVA_HOME=/var/home/core/.sdkman/candidates/java/current

# Generate Gradle wrapper (if missing)
# gradle wrapper

# Build the application
./gradlew :app:installDist
```

## Running the Application

```bash
# Run with sudo to enable packet injection
sudo -E "JAVA_HOME=$JAVA_HOME" "PATH=$PATH" app/build/install/app/bin/app
```

## Usage Guide

### Roles
1.  **Public Node (P)**: A node with a public IP that acts as a registry and signaling relay.
2.  **Host A**: A node behind a NAT that wants to receive a connection.
3.  **Host B**: A node that wants to connect to Host A using spoofing.

### CLI Commands
- `register <publicPeerId>`: Register your local gray IP/Port with the public node.
- `list <publicPeerId>`: Fetch the list of all registered peers from the public node.
- `punch <publicPeerId> <targetPeerId> <spoofedPort>`: Coordinate a hole punch via the public node.
- `spoof <remoteIp> <remotePort> <spoofIp> <spoofPort> [remotePeerId]`: Initiate a direct connection using raw packet spoofing.
- `alias <name>`: Set your display name in the chat.
- `bye`: Quit the application.

### NAT Traversal Workflow (The Spoofing Technique)

1.  **Setup Public Node**: Start an instance of the app on a public server. Note its `PeerId`.
2.  **Host A Registration**: Host A connects to P and registers:
    ```
    >> register <P_PeerId>
    ```
3.  **Host B Discovery**: Host B connects to P and fetches A's details:
    ```
    >> list <P_PeerId>
    # Note Host A's PeerId and Gray IP/Port
    ```
4.  **Signaling**: Host B coordinates the punch through P:
    ```
    >> punch <P_PeerId> <A_PeerId> 5555
    ```
    *Host A will see a notification: `Peer B is about to spoof P_IP:5555`.*
5.  **Spoofed Connection**: Host B initiates the connection to Host A's gray IP, spoofing Host P's IP and the coordinated port:
    ```
    >> spoof <A_GrayIp> <A_GrayPort> <P_IP> 5555 <A_PeerId>
    ```
6.  **Chat**: Once the connection is established, Host A and Host B can exchange messages directly.

## Project Structure
- `app`: The main application module containing libp2p implementations and the CLI.
- `traversal_utils`: Low-level services for IP/UDP packet forging and posting using Pcap4J.
- `buildSrc`: Shared build logic and convention plugins.
