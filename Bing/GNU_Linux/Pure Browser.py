import webview
import platform

SEARCH_ENGINE = 'https://www.bing.com/search?q={}'
INDEX_HTML = 'index.html'

def get_url(text):
    text = text.strip()
    if text == '':
        return INDEX_HTML
    if text.startswith('http://') or text.startswith('https://'):
        return text
    if '.' in text and ' ' not in text:
        return 'https://' + text if not text.startswith('http') else text
    return SEARCH_ENGINE.format(text)

class Api:
    def search(self, text):
        url = get_url(text)
        webview.create_window('Pure浏览器', url, width=900, height=600, js_api=Api())

    def minimize(self):
        webview.windows[-1].minimize()

    def maximize(self):
        webview.windows[-1].toggle_fullscreen()

    def close(self):
        webview.windows[-1].destroy()

if __name__ == '__main__':
    if platform.system() != 'Linux':
        print('This version is for GNU/Linux only.')
        exit(1)
    api = Api()
    window = webview.create_window('Pure浏览器', INDEX_HTML, width=900, height=600, js_api=api)
    webview.start(debug=False, http_server=True)
