# Mobile client (React Native / Expo)

`App.js` is the whole app. Create an Expo project and drop it in:

```cmd
npx --yes create-expo-app@latest darija-mobile --template blank
cd darija-mobile
npx expo install expo-speech @react-native-async-storage/async-storage
```

Replace the generated `App.js` with the one in this folder, then:

```cmd
npx expo start
```

Scan the QR code with **Expo Go** (phone and PC on the same Wi-Fi). If Expo Go complains that
you are signed in to a different account, run `npx expo login` or sign out of Expo Go.

In the app's **Settings**, use your PC's LAN address, for example
`http://192.168.1.20:8080/api/translate` (find it with `ipconfig`). `localhost` does not work from
a phone. Android emulator: `http://10.0.2.2:8080/api/translate`. Allow port 8080 in the Windows
firewall if the phone cannot connect.
