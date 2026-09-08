package com.keshu.mobile.data.portal

internal object JnuCurriculumScripts {
    val exporter =
        """
        (function() {
          if (window.__keshuJnuCourseExporterInstalled) return 'installed';
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
            var current = window.__keshuJnuJobs.courses;
            if (current && current.status === 'running') return 'running';
            window.__keshuJnuJobs.courses = {status: 'running', startedAt: Date.now()};
            Promise.resolve().then(work).then(function(payload) {
              window.__keshuJnuJobs.courses = {status: 'done', payload: payload, finishedAt: Date.now()};
            }).catch(function(error) {
              window.__keshuJnuJobs.courses = {status: 'error', message: String(error && error.message || error), finishedAt: Date.now()};
            });
            return 'started';
          }

          window.__keshuJnuReadJob = function(name) {
            return JSON.stringify(window.__keshuJnuJobs[name] || {status: 'missing'});
          };

          function titleFromNode(node, element) {
            if (element) {
              var titleElement = element.querySelector('.jsmind_node_title_span');
              if (titleElement) return titleElement.getAttribute('title') || titleElement.textContent || '';
            }
            if (!node) return '';
            if (typeof node.topic === 'string') return node.topic.replace(/<[^>]+>/g, '');
            try { return node.topic.textContent || node.topic.innerText || ''; } catch (ignored) { return ''; }
          }

          function isLeaf(node) {
            return !!node && !node.isroot && (!node.children || node.children.length === 0);
          }

          function courseParams(nodeId) {
            var params = {
              SCLBDM: '04', BYNJDM: window.CURRENT_PYFACX_BYNJDM || '',
              XH: window.CURRENT_PYFACX_USERID || '', '*order': '+SFTG',
              pageSize: 1000, pageNumber: 1
            };
            if (window.CURRENT_PYFACX_PYFADM === nodeId) params.PYFADM = nodeId;
            else {
              params.KZH = nodeId;
              params.PYFADM = window.CURRENT_PYFACX_PYFADM || '';
            }
            return params;
          }

          async function exportCourses() {
            var startedAt = Date.now();
            var rawNodes = [];
            if (window._jm && window._jm.mind && window._jm.mind.nodes) {
              Object.keys(window._jm.mind.nodes).forEach(function(key) { rawNodes.push(window._jm.mind.nodes[key]); });
            } else {
              document.querySelectorAll('jmnode[nodeid]').forEach(function(element) {
                rawNodes.push({id: element.getAttribute('nodeid'), topic: titleFromNode(null, element), children: [], _data: {view: {element: element}}});
              });
            }
            var leaves = rawNodes.filter(function(node) { return isLeaf(node) && !!node.id; });
            var groups = new Array(leaves.length);
            var failedGroups = [];
            var nextIndex = 0;
            var workerCount = Math.min(4, leaves.length);

            async function worker() {
              while (true) {
                var index = nextIndex++;
                if (index >= leaves.length) return;
                var node = leaves[index];
                var element = node._data && node._data.view && node._data.view.element;
                var rows = [];
                try {
                  var response = await postForm(
                    '/modules/xywccx/cxscfakzkchxkqkx.do',
                    courseParams(node.id),
                    '/jwapp/sys/xywccx/modules/xywccx/cxscfakzkchxkqkx.do'
                  );
                  rows = nestedRows(response, 'cxscfakzkchxkqkx');
                } catch (error) {
                  failedGroups.push(node.id);
                }
                groups[index] = {id: node.id, title: titleFromNode(node, element), rows: rows};
              }
            }

            var workers = [];
            for (var index = 0; index < workerCount; index++) workers.push(worker());
            await Promise.all(workers);
            var totalRows = groups.reduce(function(total, group) { return total + ((group && group.rows) || []).length; }, 0);
            return JSON.stringify({
              type: 'JNU_ALL_COURSES', groupCount: groups.length, totalRows: totalRows,
              failedGroups: failedGroups, durationMs: Date.now() - startedAt, groups: groups
            });
          }

          window.__keshuJnuStartCourseExport = function() { return startJob(exportCourses); };
          window.__keshuJnuCourseExporterInstalled = true;
          return 'installed';
        })();
        """.trimIndent()
}
