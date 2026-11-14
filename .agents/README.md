# Abstrak Agent Docs

This directory is the shared engineering context for the Abstrak Android app.

Current implementation snapshot:

- Foundation hardening is largely complete.
- Catalog expansion is in progress.
- The app supports audiobook detail via an overlay (glance mode) and chapter loading from the server, cached in Room.
- Real audio playback is implemented with Media3 (ExoPlayer + MediaSession) in the `:playback` module, including background playback, a media notification, progress sync/resume (offline progress queues and syncs in the background once online, furthest along winning), chapter navigation, and speed control. Downloaded tracks play from the device.
- Logout runs a single ordered flow (`LogoutUserUseCase`): stops playback, clears the session, wipes catalog/cover caches and downloads; the remembered server URL and all app settings survive.
- Continue Listening shows per-book time left at 1x, hydrated from the server progress API, and follows playback live (every progress save records it locally first, offline too).
- Still not implemented: queue UI, filtering, Android Auto, and Wear OS.

Files in this directory:

- `about_project.md`: Technical and product-level introduction to the project.
- `state.md`: Current implementation status and architectural assessment.
- `road_map.md`: Recommended development roadmap from current state to a production-ready app.
- `architecture_target.md`: Target architecture and modularization direction for phone, Android Auto, Wear OS, and offline support.
- `best_practices.md`: Engineering practices and architectural guardrails for future work.
- `milestones/`: Major roadmap milestones, each with implementation notes and exit criteria.

How to use this folder:

- Read `about_project.md` first if you are new to the project.
- Read `state.md` before making major architectural changes.
- Use `road_map.md` and the `milestones/` files to plan upcoming work.
- Treat `best_practices.md` as the default standard unless there is an explicit product or technical reason to do otherwise.
