import http.server
import socketserver
import os
import mimetypes

PORT = 3000
DIRECTORY = os.path.abspath("public")

# Ensure .apk has the official Android package MIME type
mimetypes.add_type("application/vnd.android.package-archive", ".apk")

class APKHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIRECTORY, **kwargs)

    def end_headers(self):
        # Allow cross-origin and provide direct download hints for APK
        if self.path.endswith(".apk"):
            fname = "Statsys.apk" if "Statsys" in self.path else "TRTT.apk"
            self.send_header("Content-Disposition", f'attachment; filename="{fname}"')
            self.send_header("Content-Type", "application/vnd.android.package-archive")
            self.send_header("Cache-Control", "no-cache, must-revalidate")
        self.send_header("Access-Control-Allow-Origin", "*")
        super().end_headers()

if __name__ == "__main__":
    os.chdir(DIRECTORY)
    # Enable address reuse to avoid port bind conflicts
    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("", PORT), APKHandler) as httpd:
        print(f"Serving APKs and web portal from {DIRECTORY} on port {PORT}...")
        httpd.serve_forever()
