package com.keshu.mobile.data.portal

internal object JnuPortalScripts {
    val portalReady =
        """
        (function() {
          document.querySelectorAll('[amp-id="allCanUseApps"], [data-i18n="availableApps"], .amp-aside-box-mini-item, .amp-left-tab-item').forEach(function(tab) {
            var text = (tab.textContent || '').trim();
            if (tab.getAttribute('amp-id') === 'allCanUseApps' || text.indexOf('可用应用') >= 0) tab.click();
          });
          var appCount = document.querySelectorAll('#ampPersonalAsideLeftAllCanUseApps .canUseAppFlag[amp-title], #ampPersonalAsideLeftAllCanUseApps [amp-title]').length;
          if (appCount > 0) return 'ready';
          var loginButton = document.querySelector('#ampHasNoLogin:not(.amp-hide), #ampLoginBtn');
          return loginButton && (loginButton.offsetParent !== null || loginButton.id === 'ampLoginBtn') ? 'login' : '';
        })();
        """.trimIndent()

    val discoverServiceEntries =
        """
        (function() {
          function absoluteUrl(raw) {
            if (!raw) return '';
            if (/^https?:\/\//i.test(raw) || raw.indexOf('//') === 0) {
              try { return new URL(raw, location.href).href; } catch (ignored) { return ''; }
            }
            var root = (window.AMPConstant && window.AMPConstant.requestPath) || '/';
            if (root.charAt(root.length - 1) !== '/') root += '/';
            if (raw.charAt(0) === '/') raw = raw.substring(1);
            try { return new URL(root + raw, location.origin).href; } catch (ignored) { return ''; }
          }
          var wanted = ['学业完成查询', '我的课表', '我的考试安排'];
          var entries = {};
          document.querySelectorAll('#ampPersonalAsideLeftAllCanUseApps .canUseAppFlag[amp-title], #ampPersonalAsideLeftAllCanUseApps [amp-title]').forEach(function(node) {
            var title = (node.getAttribute('amp-title') || node.getAttribute('title') || '').trim();
            if (wanted.indexOf(title) >= 0) entries[title] = absoluteUrl(node.getAttribute('amp-url') || '');
          });
          return JSON.stringify(entries);
        })();
        """.trimIndent()

    val curriculumDetailEntry =
        """
        (function() {
          if (document.querySelectorAll('jmnode[nodeid]').length >= 3) return 'already-detail';
          var text = document.body ? (document.body.innerText || '') : '';
          if (text.indexOf('基础教育课程') >= 0 || text.indexOf('专业教育课程') >= 0) return 'already-detail';
          var button = Array.prototype.slice.call(document.querySelectorAll('a,button')).find(function(node) {
            return (node.getAttribute('data-action') || node.textContent || '').indexOf('查看详情') >= 0;
          });
          if (!button) return 'missing';
          if (button.__keshuJnuClicked) return 'clicked';
          if (document.querySelector('.app-loading-show, .jqx-datatable-load[style*="display: block"], .jqx-loader[style*="display: block"]')) return 'page-busy';
          if (!button.getAttribute('pyfadm')) return 'not-ready';
          button.__keshuJnuClicked = true;
          button.scrollIntoView({block: 'center'});
          button.click();
          return 'clicked';
        })();
        """.trimIndent()
}
