package com.keshu.mobile.data.portal

internal object JnuScheduleScripts {
    val exporter =
        """
        (function() {
          if (window.__keshuJnuScheduleExporterInstalled) return 'installed';
          window.__keshuJnuJobs = window.__keshuJnuJobs || {};

          function nestedRows(response, action) {
            try { return (((response || {}).datas || {})[action] || {}).rows || []; } catch (ignored) { return []; }
          }

          function formBody(params) {
            return Object.keys(params).map(function(key) {
              return encodeURIComponent(key) + '=' + encodeURIComponent(params[key]);
            }).join('&');
          }

          async function postForm(path, params, fallbackPath) {
            var url = window.WIS_EMAP_SERV ? window.WIS_EMAP_SERV.getAbsPath(path) : fallbackPath;
            var response = await fetch(url, {
              method: 'POST',
              credentials: 'include',
              headers: {'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'},
              body: formBody(params)
            });
            if (!response.ok) throw new Error('HTTP ' + response.status + ' ' + path);
            return response.json();
          }

          function startJob(work) {
            var current = window.__keshuJnuJobs.schedule;
            if (current && current.status === 'running') return 'running';
            window.__keshuJnuJobs.schedule = {status: 'running', startedAt: Date.now()};
            Promise.resolve().then(work).then(function(payload) {
              window.__keshuJnuJobs.schedule = {status: 'done', payload: payload, finishedAt: Date.now()};
            }).catch(function(error) {
              window.__keshuJnuJobs.schedule = {status: 'error', message: String(error && error.message || error), finishedAt: Date.now()};
            });
            return 'started';
          }

          window.__keshuJnuReadJob = function(name) {
            return JSON.stringify(window.__keshuJnuJobs[name] || {status: 'missing'});
          };

          function textFromCell(cells, index) {
            var cell = cells[index];
            if (!cell) return '';
            var span = cell.querySelector('span[title]');
            return ((span && span.getAttribute('title')) || cell.textContent || '').trim();
          }

          function visibleRows() {
            var rows = [];
            document.querySelectorAll('.kblb-index-table tr[role=row], tr[id*="jqxWidget"][role=row]').forEach(function(row) {
              var cells = row.querySelectorAll('td[role=gridcell]');
              if (cells.length < 11) return;
              var courseName = textFromCell(cells, 5);
              if (!courseName) return;
              rows.push({
                BJMC: textFromCell(cells, 3), JSXM: textFromCell(cells, 4), KCM: courseName,
                KCH: textFromCell(cells, 6), KCXF: textFromCell(cells, 7),
                SKSJ: textFromCell(cells, 9), JASMC: textFromCell(cells, 10)
              });
            });
            return rows;
          }

          function termFromPage() {
            var candidates = [];
            try { if (window.pub_param) candidates.push({term: window.pub_param.xnxqdm, termName: window.pub_param.xnxqmc}); } catch (ignored) {}
            try { if (window.parentParam) candidates.push({term: window.parentParam.xnxqdm, termName: window.parentParam.xnxqmc}); } catch (ignored) {}
            var element = document.querySelector('#dqxnxq2, [name="XNXQDM"], [data-name="XNXQDM"]');
            if (element) candidates.push({term: element.getAttribute('value') || element.value || element.textContent, termName: element.textContent});
            for (var index = 0; index < candidates.length; index++) {
              var term = (candidates[index].term || '').trim();
              if (term) return {term: term, termName: (candidates[index].termName || term).trim()};
            }
            return {term: '', termName: ''};
          }

          async function currentTerm() {
            var current = termFromPage();
            if (current.term) return current;
            var response = await postForm('/modules/jshkcb/dqxnxq.do', {}, '/jwapp/sys/wdkb/modules/jshkcb/dqxnxq.do');
            var rows = nestedRows(response, 'dqxnxq');
            if (!rows.length) return current;
            return {
              term: rows[0].DM || rows[0].XNXQDM || rows[0].DM_DISPLAY || '',
              termName: rows[0].MC || rows[0].XNXQMC || rows[0].DM_DISPLAY || ''
            };
          }

          async function exportSchedule() {
            var term = await currentTerm();
            var rows = visibleRows();
            if (!rows.length) {
              var response = await postForm('/modules/xskcb/xskcb.do', {
                XNXQDM: term.term || '', pageSize: 1000, pageNumber: 1,
                '*order': '+XH,+KCH,+KXH,+SKXQ,+KSJC'
              }, '/jwapp/sys/wdkb/modules/xskcb/xskcb.do');
              rows = nestedRows(response, 'xskcb');
            }
            return JSON.stringify({type: 'JNU_SCHEDULE', term: term.term || '', termName: term.termName || term.term || '', rows: rows});
          }

          window.__keshuJnuStartScheduleExport = function() { return startJob(exportSchedule); };
          window.__keshuJnuScheduleExporterInstalled = true;
          return 'installed';
        })();
        """.trimIndent()
}
