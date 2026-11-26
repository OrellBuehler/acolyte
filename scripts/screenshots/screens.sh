# The screens of the phone store set, shared by android.sh and ios.sh: <file name>|<what to open>.
# "what to open" is a section name (home, explore, library, downloads, requests, ...)
# or title:<kind>:<tmdb id> (see ScreenshotLaunch in both apps).
SCREENS=(
  "01-home|home"
  "02-explore|explore"
  "03-title|title:movie:910004"
  "04-library|library"
  "05-downloads|downloads"
  "06-requests|requests"
)

DEMO_USER="noor"
DEMO_PASSWORD="demo"
DEMO_PORT="${DEMO_PORT:-6263}"

# The screens of the TV store sets (tvos.sh, and the Android TV script): <file name>|<what to open>.
# "popular" is New & Popular, a pushed page that is no tab.
TV_SCREENS=(
  "01-home|home"
  "02-explore|explore"
  "03-title|title:movie:910004"
  "04-new-and-popular|popular"
  "05-calendar|calendar"
  "06-requests|requests"
)
