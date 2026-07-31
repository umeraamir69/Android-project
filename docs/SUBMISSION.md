# Submission checklist (professor / markers)

Use with [`DEMO_GUIDE.md`](DEMO_GUIDE.md) and the root [`README.md`](../README.md).

## Deliverables

- [ ] Source on GitHub (`main` after PR merge, or `testing` branch linked in submission)
- [ ] Demo APK built from latest code (`./gradlew assembleDebug`)
- [ ] Demo video (one per group) following `DEMO_GUIDE.md`
- [ ] Demo email + password written in README **and** in the written report
- [ ] Tested on a physical Android phone or tablet

## What to show in the video (minimum)

1. App icon + launch  
2. Create account / sign in  
3. Record or import → processing → READY notes  
4. Transcript seek + Ask AI  
5. Library ListView + open lecture  
6. Search hit → lecture  
7. Share code OR export  
8. Help menu (authors + version)  
9. French language switch  

## Repo links

- README: project overview for markers  
- `docs/DEMO_GUIDE.md`: full feature list + video script  
- `docs/LectureLens_Architecture.md`: design  
- `docs/firebase-storage.rules`: Storage rules sample  

## Do not commit

- `local.properties` (API keys)  
- `*.apk`  
- Root `google-services.json` duplicates (use `app/google-services.json`)  
