with open(r'C:\Program Files\Python312\Lib\site-packages\yt_dlp\extractor\youtube\_base.py', 'r', encoding='utf-8') as f:
    text = f.read()

start = text.find('_CLIENTS = {')
print(text[start+2500:start+6500])
