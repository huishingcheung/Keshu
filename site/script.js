const languageButtons = document.querySelectorAll('[data-language-button]');

function applyLanguage(language, persist = true) {
  const selected = language === 'zh' ? 'zh' : 'en';
  document.documentElement.dataset.language = selected;
  document.documentElement.lang = selected === 'zh' ? 'zh-CN' : 'en';
  document.title = selected === 'zh'
    ? '课枢 · Android 学业伴侣'
    : 'Keshu · Academic companion for Android';

  languageButtons.forEach((button) => {
    button.setAttribute('aria-pressed', String(button.dataset.languageButton === selected));
  });

  document.querySelector('.language-switch')?.setAttribute(
    'aria-label',
    selected === 'zh' ? '语言' : 'Language',
  );

  if (persist) {
    try {
      localStorage.setItem('keshu-language', selected);
    } catch (_) {
      // The current page can still switch languages without persistence.
    }
  }
}

languageButtons.forEach((button) => {
  button.addEventListener('click', () => applyLanguage(button.dataset.languageButton));
});

applyLanguage(document.documentElement.dataset.language, false);
