#!/usr/bin/env bash
# Usage: VERSION_NAME=... TELEGRAM_BOT_TOKEN=... upload-telegram.sh <apk> [apk...]
set -euo pipefail

chat_id=-1003667635705
message_thread_id=1908

subject=$(git log -1 --pretty=%s | sed 's/&/\&amp;/g; s/</\&lt;/g; s/>/\&gt;/g')
caption="<b>dmt ${VERSION_NAME}</b>"$'\n'"${subject}"

curl_args=(-F "chat_id=${chat_id}" -F "message_thread_id=${message_thread_id}")
media="[]"
i=0
for apk in "$@"; do
    key="doc${i}"
    curl_args+=(-F "${key}=@${apk}")
    if [ "$i" -eq 0 ]; then
        item=$(jq -n --arg m "attach://${key}" --arg cap "$caption" \
            '{type: "document", media: $m, caption: $cap, parse_mode: "HTML"}')
    else
        item=$(jq -n --arg m "attach://${key}" '{type: "document", media: $m}')
    fi
    media=$(jq -c --argjson item "$item" '. + [$item]' <<< "$media")
    i=$((i + 1))
done

curl -fsS "${curl_args[@]}" -F "media=${media}" \
    "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMediaGroup" > /dev/null
