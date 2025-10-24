# Google Play Console Publishing Guide for UniRemote

## Pre-Publication Checklist

### ✅ Build Files Ready
- **Signed APK**: `app/build/outputs/apk/release/app-release.apk` (13.3 MB)
- **Android App Bundle**: `app/build/outputs/bundle/release/app-release.aab` (12.9 MB) ⭐ **Recommended**
- **Keystore**: `keystore/uniremote-release-key.jks` (Keep this secure!)

### ✅ App Configuration
- **Package Name**: `com.jithendranara.uniremote`
- **Version Code**: 1
- **Version Name**: 1.0.0
- **Target SDK**: 35 (Android 14)
- **Min SDK**: 26 (Android 8.0)

## Step-by-Step Play Console Setup

### 1. Create New App
1. Go to [Google Play Console](https://play.google.com/console)
2. Click "Create app"
3. Fill in:
   - **App name**: UniRemote - Universal TV Remote
   - **Default language**: English (United States)
   - **App or game**: App
   - **Free or paid**: Free
   - **Declarations**: Check all applicable boxes

### 2. App Content
1. **App category**: Tools
2. **Content rating**: Complete the questionnaire (should be "Everyone")
3. **Target audience**: 18+ (due to network permissions)

### 3. App Access
1. **App availability**: All countries
2. **Device categories**: Phone and Tablet
3. **Content guidelines**: Review and accept

### 4. Ads
- **Contains ads**: No
- **Ad serving**: Not applicable

### 5. Content Rating
- Complete the content rating questionnaire
- Should result in "Everyone" rating

### 6. Data Safety
Fill out the Data Safety form with:
- **Data collection**: No personal data collected
- **Data sharing**: No data shared with third parties
- **Data security**: Local network only
- **Permissions**: List all permissions used and their purposes

### 7. App Content
- **App details**: Use content from `app-listing.md`
- **Graphics**: Need to create screenshots and feature graphic
- **Contact details**: Add your email and website

## Required Graphics Assets

### Screenshots (Required)
Create screenshots for:
- **Phone**: 1080 x 1920 px (minimum 2, maximum 8)
- **Tablet**: 1200 x 1920 px (optional)

### Feature Graphic
- **Size**: 1024 x 500 px
- **Format**: PNG or JPEG
- **Content**: App logo with "UniRemote" text

### App Icon
- **Size**: 512 x 512 px
- **Format**: PNG
- **Note**: Already exists in the project

## Upload Process

### 1. Production Track
1. Go to "Production" track
2. Click "Create new release"
3. Upload the AAB file: `app-release.aab`
4. Add release notes:
   ```
   Initial release of UniRemote
   
   Features:
   • Universal remote for Roku TV and Fire TV
   • Smart volume and power routing
   • Automatic device discovery
   • Material 3 design
   • Floating overlay controls
   ```

### 2. Review Information
- **App bundle**: Upload `app-release.aab`
- **Release name**: 1.0.0 (1)
- **Release notes**: Use the content above

### 3. Review and Rollout
1. Review all information
2. Click "Review release"
3. Submit for review

## Important Notes

### Keystore Security
- **CRITICAL**: Keep `keystore/uniremote-release-key.jks` secure
- **Backup**: Store keystore in multiple secure locations
- **Password**: Store passwords securely (currently: `uniremote123`)
- **Future updates**: You'll need this keystore for all future updates

### Network Permissions
The app uses several network-related permissions that may require justification:
- `INTERNET`: For local network communication
- `CHANGE_WIFI_MULTICAST_STATE`: For Roku device discovery
- `SYSTEM_ALERT_WINDOW`: For floating overlay (optional)

### Testing Recommendations
Before publishing:
1. Test on multiple devices
2. Test on different Android versions (8.0+)
3. Test network discovery functionality
4. Test Fire TV integration (if applicable)

## Post-Publication

### Monitoring
- Check Play Console for crash reports
- Monitor user reviews
- Track download statistics

### Updates
For future updates:
1. Increment version code and version name
2. Build new release with same keystore
3. Upload to Play Console
4. Submit for review

## Troubleshooting

### Common Issues
1. **Keystore errors**: Ensure keystore file and passwords are correct
2. **Permission issues**: Justify all permissions in Data Safety section
3. **Content rating**: Complete all required questionnaires
4. **Graphics**: Ensure all required graphics are uploaded

### Support
- **Documentation**: Check README.md for user instructions
- **Issues**: Use GitHub Issues for bug reports
- **Contact**: Provide support email in Play Console

## Timeline
- **Review time**: 1-3 days (typically)
- **Publication**: Automatic after approval
- **Rollout**: Can be gradual or immediate

---

**Remember**: This is your first release, so take time to ensure everything is correct. You can always update the app later, but the initial impression matters!
