# Privacy Policy for UniRemote

**Effective Date:** October 24, 2024

## Introduction

UniRemote ("we," "our," or "us") is committed to protecting your privacy. This Privacy Policy explains how we collect, use, and safeguard your information when you use our mobile application.

## Information We Collect

### Local Network Communication Only
UniRemote is designed to work entirely within your local network. We do not collect, store, or transmit any personal data to external servers.

### Device Information
- **Roku Device Discovery**: The app scans your local network to discover Roku devices using SSDP (Simple Service Discovery Protocol)
- **Fire TV Discovery**: The app discovers Fire TV devices on your local network using Amazon Fling SDK
- **Network Configuration**: IP addresses and device identifiers are stored locally on your device

### Permissions Used
- **INTERNET**: Required for local network communication with Roku and Fire TV devices
- **ACCESS_NETWORK_STATE**: To check network connectivity
- **ACCESS_WIFI_STATE**: To verify Wi-Fi connection
- **CHANGE_WIFI_MULTICAST_STATE**: Required for Roku device discovery
- **SYSTEM_ALERT_WINDOW**: For floating overlay functionality (optional)
- **FOREGROUND_SERVICE**: For persistent overlay services
- **PACKAGE_USAGE_STATS**: To detect Fire TV app usage (optional)

## How We Use Your Information

All information collected is used solely for:
- Discovering and connecting to Roku and Fire TV devices on your local network
- Sending remote control commands to your devices
- Providing floating overlay functionality
- Storing your preferences locally on your device

## Data Storage

- All settings and preferences are stored locally on your device using Android DataStore
- No data is transmitted to external servers
- No cloud storage or synchronization is used

## Third-Party Services

### Amazon Fling SDK
- Used for Fire TV discovery and control
- Operates entirely within your local network
- No data is sent to Amazon servers by our app

### Home Assistant Integration (Optional)
- If you choose to use Home Assistant integration, communication is between your device and your local Home Assistant instance
- We do not have access to your Home Assistant data

## Data Security

- All communication occurs within your local network
- No external network connections are made
- Local data is protected by Android's built-in security mechanisms

## Children's Privacy

UniRemote does not collect any personal information from children under 13. The app is designed for local network use only.

## Changes to This Privacy Policy

We may update this Privacy Policy from time to time. We will notify you of any changes by posting the new Privacy Policy in the app and updating the "Effective Date" at the top of this policy.

## Contact Us

If you have any questions about this Privacy Policy, please contact us at:
- Email: [Your email here]
- GitHub: https://github.com/Jithendranara/UniRemote

## Compliance

This app complies with:
- Google Play Store policies
- Android privacy guidelines
- Local network communication best practices

---

**Note**: This privacy policy reflects the current functionality of UniRemote. As the app only operates within your local network and does not collect or transmit personal data, it has minimal privacy implications.
