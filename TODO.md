

- [ ] Reformat this document with more context and proper acceptance criteria
- Geo localization improvements
  - [ ] Improve the app notification logic about high energy drain.
        The goal is to have one notification the user can press to kill the app to avoid battery drain.
        Maybe as simple as having a "stop" button in the notification?
  - [ ] Have some logic to kickstart the GPS fix phase before starting to record. The idea is that a
        user may want to start riding as soon as they tab "record", which is harder to achieve if they
        need to wait for the GPS fix.
- [ ] On the ride screen, we should mute the colors of the GPS/IMU/Battery icon a bit. Right now the
      visual design make it looks like they are interactable (which isn't the aim).
- Settings improvements
  - [ ] Dark theme is a toggle. How can we offer to the user the choice light/dark/auto (follow system)
  - [ ] The Auto-pause menu option isn't super clear. How can we provide an explanation
    to the user about what that option entail?
- Trips
  - [ ] We currently don't have the ability to delete trips. We should have it. 
  - [ ] The trip summary has the opposite issue as the home screen in terms of discovering what elements
        are interactable with. For example, I didn't realize I had to tap the top speed to open the map.
        I only found it because I knew the map was somewhere in there and tapped everything.
- Segments
  - [ ] The concept of segment is somewhat hidden. How can we make it more apparent to the user?
        Would a shortcut in Trips or Stats be useful? Needs UI/UX investigation first.
- [ ] Rename app to be named Speed instead of "Race Logger"
  - Debug builds should have a different package and name (suffixed " (debug)") so that I can run both
    variants for testing
- [ ] Investigate what app > battery > optimized or unrestricted on Samsung phones mean in terms of
      generic Android, and if the app needs to worry/prompt the user about it. Maybe less precise access
      to the GPS in the background (e.g. phone screen is turned off) ?
