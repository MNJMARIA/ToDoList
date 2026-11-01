# Вхід і реєстрація через Google: одноразове налаштування

Код кнопки «Продовжити через Google» уже є в додатку. Щоб вона запрацювала, у проєкті Firebase треба зробити чотири речі (код цього не зробить за вас). Поки цього немає, кнопка показує повідомлення «Вхід через Google ще не налаштований у цій збірці», а вхід за email і паролем працює як раніше.

## 1. Увімкнути Google у Firebase

Firebase Console → **Authentication → Sign-in method → Google → Enable**. Оберіть email підтримки й натисніть **Save**.

## 2. Дізнатися відбитки (SHA-1 і SHA-256) ключа, яким підписано реліз

У PowerShell у папці проєкту (змінна `$kt` — шлях до `keytool.exe` з Android Studio, як при створенні ключа):

```powershell
& $kt -list -v -keystore .\todolist-release.jks -alias todolist
```

Введіть пароль ключа. У виводі знайдіть рядки `SHA1:` і `SHA256:`.

Якщо додаток запускається ще й з Android Studio (debug-збірка), потрібні відбитки debug-ключа:

```powershell
& $kt -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

## 3. Додати відбитки у Firebase

Firebase Console → **Project settings (шестерня) → Your apps → додаток `com.example.todolist` → Add fingerprint**. Додайте SHA-1 і SHA-256 **релізного** ключа (і, за потреби, debug-ключа).

## 4. Оновити `google-services.json`

Там само натисніть **Download google-services.json**. Замініть файл `app/google-services.json` у проєкті. Перевірте, що в новому файлі є блок `oauth_client` з `"client_type": 3` (це «веб-клієнт», який додаток використовує як `default_web_client_id`). Закомітьте й запушіть файл.

Далі випустіть нову версію (наприклад, `git tag v1.3.1`, `git push origin v1.3.1`; номер має бути більшим за вже випущений) — див. `RELEASING.uk.md`.

## Якщо не працює

| Що видно | Причина |
|---|---|
| «Вхід через Google ще не налаштований у цій збірці» | у `google-services.json` немає веб-клієнта: повторіть кроки 1 і 4 |
| «На телефоні не знайдено акаунта Google» | на телефоні немає акаунта Google (додайте в налаштуваннях), або не додано відбиток SHA-1 (крок 3) |
| «Не вдалося увійти через Google» і в дужках щось про `10`, `12500` чи `28444` | відбиток ключа, яким підписано цей APK, не доданий у Firebase (крок 3) |
| Працює з Android Studio, не працює з релізу (або навпаки) | у Firebase додано відбитки лише одного з двох ключів |

## Як поводиться вхід

- Одна кнопка і для входу, і для першої реєстрації. Для нового акаунта профіль (ім'я з Google, логін з email) створюється автоматично, і додаток одразу відкриває головний екран.
- Якщо email уже зареєстрований із паролем, Firebase за замовчуванням об'єднує акаунти (в налаштуванні Authentication → Settings → «One account per email address»).
