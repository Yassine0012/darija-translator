import { useEffect, useState } from 'react';
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import * as Speech from 'expo-speech';
import AsyncStorage from '@react-native-async-storage/async-storage';

const SETTINGS_KEY = 'darija-settings';
const DEFAULT_SETTINGS = {
  // On a real phone, replace with your PC's LAN IP (e.g. http://192.168.1.20:8080/api/translate).
  // Android emulator: http://10.0.2.2:8080/api/translate
  url: 'http://192.168.1.20:8080/api/translate',
  user: 'admin',
  password: '',
};
const SOURCES = [
  { code: '', label: 'Auto' },
  { code: 'en', label: 'English' },
  { code: 'fr', label: 'French' },
  { code: 'es', label: 'Spanish' },
];

export default function App() {
  const [settings, setSettings] = useState(DEFAULT_SETTINGS);
  const [showSettings, setShowSettings] = useState(false);
  const [source, setSource] = useState('');
  const [text, setText] = useState('');
  const [result, setResult] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    AsyncStorage.getItem(SETTINGS_KEY).then((saved) => {
      if (saved) setSettings({ ...DEFAULT_SETTINGS, ...JSON.parse(saved) });
      else setShowSettings(true); // first launch: ask for server and credentials
    });
  }, []);

  const saveSettings = async () => {
    await AsyncStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
    setShowSettings(false);
  };

  const translate = async () => {
    if (!text.trim()) return;
    setLoading(true);
    setError('');
    setResult('');

    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 60000);
    try {
      const response = await fetch(settings.url, {
        method: 'POST',
        signal: controller.signal,
        headers: {
          'Content-Type': 'application/json',
          Authorization: 'Basic ' + btoa(`${settings.user}:${settings.password}`),
        },
        body: JSON.stringify({ text: text.trim(), source }),
      });
      if (response.status === 401) {
        setShowSettings(true);
        throw new Error('Authentication failed: check username and password.');
      }
      if (!response.ok) throw new Error(`Server error (HTTP ${response.status}).`);
      const data = await response.json();
      setResult(data.translation);
    } catch (e) {
      setError(
        e instanceof TypeError || e.name === 'AbortError'
          ? 'Cannot reach the server. Check the URL in Settings, that the server is running, and that the phone is on the same Wi-Fi.'
          : e.message
      );
    } finally {
      clearTimeout(timer);
      setLoading(false);
    }
  };

  const speak = () => {
    if (!result) return;
    Speech.stop();
    Speech.speak(result, { language: 'ar' });
  };

  return (
    <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
        <Text style={styles.title}>Darija Translator</Text>

        <View style={styles.chips}>
          {SOURCES.map((s) => (
            <TouchableOpacity
              key={s.code}
              style={[styles.chip, source === s.code && styles.chipActive]}
              onPress={() => setSource(s.code)}
            >
              <Text style={source === s.code ? styles.chipTextActive : styles.chipText}>{s.label}</Text>
            </TouchableOpacity>
          ))}
        </View>

        <TextInput
          style={[styles.input, styles.textArea]}
          multiline
          placeholder="Text to translate"
          value={text}
          onChangeText={setText}
        />

        <View style={styles.row}>
          <TouchableOpacity style={styles.button} onPress={translate} disabled={loading}>
            <Text style={styles.buttonText}>Translate</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.button, styles.secondary]}
            onPress={() => { setText(''); setResult(''); setError(''); }}
          >
            <Text style={styles.secondaryText}>Clear</Text>
          </TouchableOpacity>
        </View>

        {loading && <ActivityIndicator style={styles.gap} size="large" />}
        {!!error && <Text style={styles.error}>{error}</Text>}

        {!!result && (
          <View style={styles.gap}>
            <Text style={styles.result}>{result}</Text>
            <TouchableOpacity style={[styles.button, styles.secondary]} onPress={speak}>
              <Text style={styles.secondaryText}>🔊 Read aloud</Text>
            </TouchableOpacity>
          </View>
        )}

        <TouchableOpacity style={styles.gap} onPress={() => setShowSettings(!showSettings)}>
          <Text style={styles.link}>{showSettings ? '▾ Settings' : '▸ Settings'}</Text>
        </TouchableOpacity>

        {showSettings && (
          <View>
            <Text style={styles.label}>Endpoint URL</Text>
            <TextInput
              style={styles.input}
              autoCapitalize="none"
              autoCorrect={false}
              value={settings.url}
              onChangeText={(url) => setSettings({ ...settings, url })}
            />
            <Text style={styles.label}>Username</Text>
            <TextInput
              style={styles.input}
              autoCapitalize="none"
              autoCorrect={false}
              value={settings.user}
              onChangeText={(user) => setSettings({ ...settings, user })}
            />
            <Text style={styles.label}>Password</Text>
            <TextInput
              style={styles.input}
              secureTextEntry
              autoCapitalize="none"
              value={settings.password}
              onChangeText={(password) => setSettings({ ...settings, password })}
            />
            <TouchableOpacity style={[styles.button, styles.gap]} onPress={saveSettings}>
              <Text style={styles.buttonText}>Save settings</Text>
            </TouchableOpacity>
          </View>
        )}
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: '#fff' },
  container: { padding: 20, paddingTop: 60 },
  title: { fontSize: 24, fontWeight: '700', marginBottom: 16 },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginBottom: 12 },
  chip: { paddingVertical: 6, paddingHorizontal: 12, borderRadius: 16, backgroundColor: '#e8eaed' },
  chipActive: { backgroundColor: '#1a73e8' },
  chipText: { color: '#1f2933' },
  chipTextActive: { color: '#fff' },
  input: { borderWidth: 1, borderColor: '#c4ccd4', borderRadius: 8, padding: 10, fontSize: 16 },
  textArea: { minHeight: 110, textAlignVertical: 'top' },
  row: { flexDirection: 'row', gap: 8, marginTop: 12 },
  button: { backgroundColor: '#1a73e8', paddingVertical: 10, paddingHorizontal: 18, borderRadius: 8, alignItems: 'center' },
  buttonText: { color: '#fff', fontSize: 16 },
  secondary: { backgroundColor: '#e8eaed' },
  secondaryText: { color: '#1f2933', fontSize: 16 },
  gap: { marginTop: 16 },
  result: { fontSize: 24, lineHeight: 38, textAlign: 'right', writingDirection: 'rtl', marginBottom: 12 },
  error: { color: '#c5221f', marginTop: 12 },
  label: { fontWeight: '600', marginTop: 12, marginBottom: 4 },
  link: { color: '#1a73e8', fontSize: 16, fontWeight: '600' },
});
