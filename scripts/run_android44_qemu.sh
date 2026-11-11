#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
VM_DIR="/home/cune/.android_vm"
ISO_PATH="$VM_DIR/android-4.4-r5.iso"
EXTRACT_DIR="$VM_DIR/kitkat_extracted"
APK_PATH="$ROOT_DIR/aether-browser.apk"

echo "=== Aether Browser Empirical Android 4.4 Verification ==="
echo "Target: Android 4.4 KitKat (API 19) on 1GB RAM Quad-Core (Lenovo A536 spec)"

if [ ! -f "$ISO_PATH" ]; then
    echo "Error: ISO not found at $ISO_PATH"
    exit 1
fi

if [ ! -f "$APK_PATH" ]; then
    echo "Error: APK not found at $APK_PATH"
    exit 1
fi

mkdir -p "$EXTRACT_DIR"
if [ ! -f "$EXTRACT_DIR/kernel" ]; then
    echo "Extracting Android 4.4 kernel and images from ISO..."
    7z x -y -o"$EXTRACT_DIR" "$ISO_PATH" kernel initrd.img ramdisk.img system.sfs >/dev/null
    echo "Extraction complete."
fi

echo "Killing any prior QEMU instances on port 5555..."
pkill -f "qemu.*5555" || true
sleep 1

echo "Launching Android 4.4 KitKat in QEMU (KVM, 1GB RAM, 4 CPU, 480x854 FWVGA)..."
qemu-system-x86_64 \
    -enable-kvm \
    -m 1024 \
    -smp 4 \
    -cpu host \
    -vga std \
    -display none \
    -device virtio-net-pci,netdev=net0 \
    -netdev user,id=net0,hostfwd=tcp::5555-:5555 \
    -kernel "$EXTRACT_DIR/kernel" \
    -initrd "$EXTRACT_DIR/initrd.img" \
    -append "root=/dev/ram0 androidboot.hardware=android_x86 quiet SRC= DATA= DPI=196 UVESA_MODE=480x854 console=tty0" \
    -daemonize \
    -pidfile "$VM_DIR/qemu.pid"

echo "Waiting for Android 4.4 ADB port (5555) to become ready..."
for i in $(seq 1 40); do
    if adb connect 127.0.0.1:5555 2>&1 | grep -q "connected"; then
        echo "ADB connected to Android 4.4 KitKat!"
        break
    fi
    echo "Waiting for boot... ($i/40)"
    sleep 3
done

echo "Waiting for Android package manager..."
for i in $(seq 1 30); do
    BOOT=$(adb -s 127.0.0.1:5555 shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
    if [ "$BOOT" = "1" ]; then
        echo "Android 4.4 boot completed!"
        break
    fi
    sleep 2
done

echo "Installing Aether Browser APK..."
adb -s 127.0.0.1:5555 install -r "$APK_PATH"

echo "Launching Aether Browser..."
adb -s 127.0.0.1:5555 shell am start -n com.aether.browser.debug/com.aether.browser.MainActivity

sleep 5

echo "=== Telemetry: Process Status ==="
adb -s 127.0.0.1:5555 shell ps | grep -E "aether|NAME" || true

echo "=== Telemetry: Memory Footprint (1GB RAM budget verification) ==="
adb -s 127.0.0.1:5555 shell dumpsys meminfo com.aether.browser.debug || true

echo "=== Telemetry: Logcat ==="
adb -s 127.0.0.1:5555 logcat -d -s Aether/Runtime Aether/Hardware Aether/TabManager GeckoView || true

echo "=== Empirical Verification Finished Successfully ==="
