<?php
// Minimal PHP client for the Darija translator REST service.
// Run:  php -S localhost:8000     then open http://localhost:8000
// Configuration through environment variables: API_URL, API_USER, API_PASSWORD.

$apiUrl  = getenv('API_URL') ?: 'http://localhost:8080/api/translate';
$apiUser = getenv('API_USER') ?: 'admin';
$apiPass = getenv('API_PASSWORD') ?: '';

$text = trim($_POST['text'] ?? '');
$source = $_POST['source'] ?? '';
$translation = null;
$error = null;

if ($text !== '') {
    $ch = curl_init($apiUrl);
    curl_setopt_array($ch, [
        CURLOPT_POST           => true,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_TIMEOUT        => 60,
        CURLOPT_USERPWD        => "$apiUser:$apiPass",
        CURLOPT_HTTPAUTH       => CURLAUTH_BASIC,
        CURLOPT_HTTPHEADER     => ['Content-Type: application/json'],
        CURLOPT_POSTFIELDS     => json_encode(['text' => $text, 'source' => $source], JSON_UNESCAPED_UNICODE),
    ]);
    $body = curl_exec($ch);
    $status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $curlError = curl_error($ch);
    curl_close($ch);

    if ($body === false) {
        $error = "Cannot reach the server: $curlError";
    } elseif ($status === 401) {
        $error = 'Authentication failed: check API_USER and API_PASSWORD.';
    } elseif ($status !== 200) {
        $error = "Server error (HTTP $status).";
    } else {
        $translation = json_decode($body, true)['translation'] ?? null;
    }
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Darija Translator (PHP client)</title>
  <style>
    body { font-family: system-ui, sans-serif; max-width: 640px; margin: 40px auto; padding: 0 16px; }
    textarea, select { width: 100%; padding: 8px; font: inherit; box-sizing: border-box; margin: 6px 0; }
    textarea { min-height: 110px; }
    button { padding: 8px 16px; font: inherit; }
    .result { margin-top: 16px; padding: 12px; background: #f1f5f9; border-radius: 6px; font-size: 20px; }
    .error { margin-top: 16px; color: #b00020; }
  </style>
</head>
<body>
  <h1>Darija Translator</h1>
  <form method="post">
    <select name="source">
      <?php foreach (['' => 'Auto-detect', 'en' => 'English', 'fr' => 'French', 'es' => 'Spanish'] as $code => $label): ?>
        <option value="<?= $code ?>" <?= $source === $code ? 'selected' : '' ?>><?= $label ?></option>
      <?php endforeach; ?>
    </select>
    <textarea name="text" placeholder="Text to translate"><?= htmlspecialchars($text) ?></textarea>
    <button type="submit">Translate</button>
  </form>

  <?php if ($translation !== null): ?>
    <div class="result" dir="rtl" lang="ar"><?= htmlspecialchars($translation) ?></div>
  <?php endif; ?>
  <?php if ($error !== null): ?>
    <div class="error"><?= htmlspecialchars($error) ?></div>
  <?php endif; ?>
</body>
</html>
