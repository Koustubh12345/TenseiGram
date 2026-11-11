import urllib.request, json, urllib.parse

prompt = "Fix all grammar, spelling, punctuation, capitalization, and phrasing in the following text. Output ONLY the corrected text. Do NOT add quotes, notes, or explanations: he go to school yesterday"

# Test 1: Pollinations with model=openai or seed
try:
    url = f"https://text.pollinations.ai/{urllib.parse.quote(prompt)}?model=openai"
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req, timeout=5) as resp:
        print("Pollinations openai:", resp.read().decode('utf-8'))
except Exception as e:
    print("Pollinations error:", e)

# Test 2: LanguageTool
try:
    req = urllib.request.Request(
        "https://api.languagetool.org/v2/check",
        data=urllib.parse.urlencode({'text': 'he go to school yesterday', 'language': 'en-US'}).encode('utf-8'),
        headers={'Content-Type': 'application/x-www-form-urlencoded', 'User-Agent': 'Mozilla/5.0'}
    )
    with urllib.request.urlopen(req, timeout=4) as resp:
        data = json.loads(resp.read().decode('utf-8'))
        print("LanguageTool matches:", len(data.get('matches', [])))
        for m in data.get('matches', []):
            print("   rule:", m.get('message'), "replacements:", [r.get('value') for r in m.get('replacements', [])][:3])
except Exception as e:
    print("LanguageTool error:", e)

# Test 3: Free AI APIs
endpoints = [
    'https://freeaichat.xyz/api/chat',
    'https://api.deepinfra.com',
    'https://api.airforce'
]
