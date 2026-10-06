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

**Occurrence Date（发生日期）**:
The calendar date on which a particular occurrence of an event falls. Different cycles of a repeating event have their own occurrence dates.
_Avoid_: original date, reminder time

**Desktop Widget（桌面小组件）**:
A view placed on the Android home screen for reading a selected event or a list of upcoming events.
_Avoid_: in-app control, notification

**Widget Instance（小组件实例）**:
One independently configured desktop widget placed on a particular device.
_Avoid_: widget type, account preference

**Upcoming Events（近期事件）**:
Events whose known occurrence dates fall within a specified window starting today. Past occurrences and occurrences awaiting date confirmation are not included.
_Avoid_: pinned events, all future events

**Account Backup（账号数据备份）**:
A copy of an account's categories, events, sub-events, and reminder channel information. It is distinct from a backup of device-local reminders and desktop configuration.
_Avoid_: complete device backup, offline snapshot

**Account Data（账号数据）**:
The profile, categories, events, sub-events, and reminder channels belonging to an account. The web and Android clients access the same account data when connected to the same service.
_Avoid_: device preferences, offline snapshot

**Device Preferences（设备偏好）**:
Appearance, layout, and local-use choices belonging to a particular browser or Android device. They may differ between devices using the same account.
_Avoid_: account data, shared account settings
