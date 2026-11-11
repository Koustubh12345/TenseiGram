import urllib.request, json, re

vid = 'zKYNITx17Mw'

req_web = urllib.request.Request(f'https://www.youtube.com/watch?v={vid}', headers={'User-Agent': 'Mozilla/5.0'})
cookies = []
visitor_data = None
with urllib.request.urlopen(req_web) as resp:
    html = resp.read().decode('utf-8', errors='ignore')
    for h, v in resp.getheaders():
        if h.lower() == 'set-cookie':
            cookies.append(v.split(';')[0])
    m = re.search(r'\"visitorData\":\s*\"([^\"]+)\"', html)
    if m: visitor_data = m.group(1)

cookie_header = "; ".join(cookies)

clients_to_test = [
    {'name': 'TVHTML5', 'version': '7.20250212.14.00'},
    {'name': 'TVHTML5_SIMPLY_EMBEDDED_PLAYER', 'version': '2.0'},
    {'name': 'MWEB', 'version': '2.20260708.05.00'},
    {'name': 'WEB', 'version': '2.20260708.00.00'},
    {'name': 'ANDROID_EMBEDDED_PLAYER', 'version': '19.09.37'},
    {'name': 'KIDS', 'version': '8.26.1'},
    {'name': 'WEB_EMBEDDED_PLAYER', 'version': '2.20260708.00.00'},
]

for c in clients_to_test:
    client_dict = {
        'clientName': c['name'],
        'clientVersion': c['version'],
        'hl': 'en',
        'gl': 'US'
    }
    if visitor_data: client_dict['visitorData'] = visitor_data

    payload = {
        'videoId': vid,
        'context': {'client': client_dict}
    }
    if 'EMBEDDED' in c['name']:
        payload['context']['thirdParty'] = {'embedUrl': 'https://www.google.com/'}

    req = urllib.request.Request(
        'https://www.youtube.com/youtubei/v1/player',
        data=json.dumps(payload).encode('utf-8'),
        headers={'Content-Type': 'application/json', 'User-Agent': 'Mozilla/5.0', 'Cookie': cookie_header}
    )
    try:
        with urllib.request.urlopen(req, timeout=5) as resp:
            data = json.loads(resp.read().decode('utf-8'))
            fmts = data.get('streamingData', {}).get('formats', [])
            adaptive = data.get('streamingData', {}).get('adaptiveFormats', [])
            status = data.get('playabilityStatus', {}).get('status')
            print(f"Client {c['name']}: status={status} fmts={len(fmts)} adaptive={len(adaptive)}")
            if fmts:
                for f in fmts:
                    print(f"   FMT itag={f.get('itag')} url={'url' in f} quality={f.get('qualityLabel')}")
                    if 'url' in f:
                        print("   Direct progressive URL:", f['url'][:80])
    except Exception as e:
        print(f"Client {c['name']} error: {e}")
