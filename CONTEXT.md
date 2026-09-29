# Days Matter Android

A countdown and event tracking app. People organize events by category, see the number of days to each date, and configure remote or device-local reminders.

## Language

**Category（分类）**:
An event grouping with a name, color, and icon identifier.
_Avoid_: folder, tag

**Event（事件）**:
A dated countdown item that belongs to one category and may repeat or contain sub-events.
_Avoid_: anniversary (an event purpose, not the domain term)

**Icon Identifier（图标标识符）**:
A short stored value, such as `briefcase`, that identifies a category icon.
_Avoid_: emoji, file path, URL

**Reminder Channel（提醒渠道）**:
An account-owned destination for sending messages outside the app, referenced by events.
_Avoid_: local reminder, phone notification

**Local Reminder（本地提醒）**:
A system notification scheduled by one Android device for an event.
_Avoid_: reminder channel, Webhook
