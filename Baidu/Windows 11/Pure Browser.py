import webview

SEARCH_ENGINE = 'https://www.baidu.com/s?wd={}'
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
        try:
            import ctypes
            user32 = ctypes.windll.user32
            class RECT(ctypes.Structure):
                _fields_ = [('left', ctypes.c_long), ('top', ctypes.c_long), ('right', ctypes.c_long), ('bottom', ctypes.c_long)]
            rect = RECT()
            SPI_GETWORKAREA = 0x0030
            user32.SystemParametersInfoW(SPI_GETWORKAREA, 0, ctypes.byref(rect), 0)
            x, y = rect.left, rect.top
            w, h = rect.right - rect.left, rect.bottom - rect.top
            webview.windows[-1].resize(w, h)
            webview.windows[-1].move(x, y)
        except Exception:
            webview.windows[-1].toggle_fullscreen()

    def close(self):
        webview.windows[-1].destroy()

if __name__ == '__main__':
    api = Api()
    window = webview.create_window('Pure浏览器', INDEX_HTML, width=900, height=600, js_api=api)
    webview.start(gui='edgechromium', debug=False, http_server=True)
