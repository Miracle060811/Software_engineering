"""Forward a loopback-only Kind proxy to Docker Desktop's internal proxy."""

from __future__ import annotations

import socket
import socketserver
import sys
import threading


LISTEN_HOST = "127.0.0.1"
TARGET_HOST = "http.docker.internal"
TARGET_PORT = 3128


def copy_stream(source: socket.socket, destination: socket.socket) -> None:
    try:
        while data := source.recv(64 * 1024):
            destination.sendall(data)
    except OSError:
        pass
    finally:
        try:
            destination.shutdown(socket.SHUT_WR)
        except OSError:
            pass


class ProxyHandler(socketserver.BaseRequestHandler):
    def handle(self) -> None:
        with socket.create_connection((TARGET_HOST, TARGET_PORT), timeout=10) as upstream:
            downstream_to_upstream = threading.Thread(
                target=copy_stream, args=(self.request, upstream), daemon=True
            )
            downstream_to_upstream.start()
            copy_stream(upstream, self.request)
            downstream_to_upstream.join(timeout=2)


class ThreadingProxy(socketserver.ThreadingTCPServer):
    allow_reuse_address = True
    daemon_threads = True


def main() -> None:
    if len(sys.argv) != 2 or not sys.argv[1].isdigit():
        raise SystemExit("usage: kind-proxy-forwarder.py <listen-port>")
    port = int(sys.argv[1])
    if not 1 <= port <= 65535:
        raise SystemExit("listen port must be between 1 and 65535")
    with ThreadingProxy((LISTEN_HOST, port), ProxyHandler) as server:
        server.serve_forever()


if __name__ == "__main__":
    main()
