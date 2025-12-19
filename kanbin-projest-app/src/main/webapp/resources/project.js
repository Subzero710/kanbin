document.addEventListener("DOMContentLoaded", function () {
    console.log("Kanbin Project JS Loaded");

    function postFormUrlEncoded(url, params) {
        // 1. Récupération des tokens de sécurité depuis le HTML
        const metaToken = document.querySelector('meta[name="_csrf"]');
        const metaHeader = document.querySelector('meta[name="_csrf_header"]');

        const headers = {
            'Content-Type': 'application/x-www-form-urlencoded'
        };

        // 2. Injection du token dans les headers si les balises existent
        if (metaToken && metaHeader) {
            headers[metaHeader.getAttribute('content')] = metaToken.getAttribute('content');
        }

        return fetch(url, {
            method: 'POST',
            headers: headers, // Utilisation des headers sécurisés
            body: new URLSearchParams(params).toString()
        });
    }

    // ------------------------------------------------------------
    // 1) DRAG COLONNES : persist sur /board/reorder-column + sync UI
    // ------------------------------------------------------------

    const headerContainer = document.querySelector('.column-headers-container');
    const headerItems = Array.from(document.querySelectorAll('.col-header-item'));

    let draggedColumn = null;
    let headerOrderBefore = null; // rollback (liste des data-column-id)

    function getHeaderItems() {
        return Array.from(document.querySelectorAll('.col-header-item'));
    }

    function getHeaderOrderIds() {
        return getHeaderItems().map(el => el.getAttribute('data-column-id'));
    }

    function getHeaderOrderKeys() {
        return getHeaderItems().map(el => el.getAttribute('data-col'));
    }

    function syncSwimlanesToHeader() {
        const desiredKeys = getHeaderOrderKeys();
        document.querySelectorAll('.swimlane-columns-container').forEach(container => {
            const sections = Array.from(container.children);
            const map = new Map();
            sections.forEach(sec => {
                const key = sec.getAttribute('data-col');
                if (key) map.set(key, sec);
            });

            desiredKeys.forEach(key => {
                const sec = map.get(key);
                if (sec) container.appendChild(sec);
            });
        });
    }

    function restoreHeaderOrderByIds(ids) {
        if (!headerContainer) return;
        const current = new Map();
        getHeaderItems().forEach(el => current.set(el.getAttribute('data-column-id'), el));
        ids.forEach(id => {
            const el = current.get(id);
            if (el) headerContainer.appendChild(el);
        });
    }

    if (headerContainer) {
        headerItems.forEach(item => {

            // draggable seulement si draggable="true"
            if (item.getAttribute('draggable') === 'true') {
                item.addEventListener('dragstart', function (e) {
                    draggedColumn = this;
                    headerOrderBefore = getHeaderOrderIds();
                    e.dataTransfer.effectAllowed = 'move';
                    this.style.opacity = '0.4';
                });

                item.addEventListener('dragend', function () {
                    this.style.opacity = '1';
                    draggedColumn = null;
                    headerItems.forEach(c => (c.style.border = ""));
                });
            }

            // tout header item peut être une target
            item.addEventListener('dragover', function (e) {
                if (!draggedColumn) return;
                e.preventDefault();
                return false;
            });

            item.addEventListener('dragenter', function () {
                if (!draggedColumn || this === draggedColumn) return;
                this.style.border = "2px dashed #666";
            });

            item.addEventListener('dragleave', function () {
                this.style.border = "";
            });

            item.addEventListener('drop', function (e) {
                if (!draggedColumn) return false;

                e.preventDefault();
                e.stopPropagation();
                this.style.border = "";

                if (draggedColumn === this) return false;

                const parent = this.parentNode;
                if (!parent) return false;

                const BACKLOG = 'backlog';
                const CLOSED = 'closed';

                const targetKey = this.getAttribute('data-col');

                // garde-fous : pas avant backlog, pas après closed
                if (targetKey === BACKLOG) {
                    parent.insertBefore(draggedColumn, this.nextSibling);
                } else if (targetKey === CLOSED) {
                    parent.insertBefore(draggedColumn, this);
                } else {
                    const all = Array.from(parent.children);
                    const iDragged = all.indexOf(draggedColumn);
                    const iTarget = all.indexOf(this);
                    if (iDragged < iTarget) parent.insertBefore(draggedColumn, this.nextSibling);
                    else parent.insertBefore(draggedColumn, this);
                }

                // clamp avant closed si présent
                const closedEl = parent.querySelector(`.col-header-item[data-col='${CLOSED}']`);
                if (closedEl && draggedColumn !== closedEl) {
                    const all = Array.from(parent.children);
                    if (all.indexOf(draggedColumn) > all.indexOf(closedEl)) {
                        parent.insertBefore(draggedColumn, closedEl);
                    }
                }

                // aligner toutes les swimlanes immédiatement
                syncSwimlanesToHeader();

                // persist serveur
                const columnId = draggedColumn.getAttribute('data-column-id');
                const newIndex = Array.from(parent.children).indexOf(draggedColumn);

                postFormUrlEncoded('/board/reorder-column', {
                    columnId: String(columnId),
                    newIndex: String(newIndex)
                })
                    .then(async (resp) => {
                        const txt = await resp.text().catch(() => '');
                        if (!resp.ok || txt.trim() !== 'OK') {
                            throw new Error(txt || 'Erreur serveur');
                        }
                    })
                    .catch((err) => {
                        console.error('Erreur reorder-column:', err);
                        if (headerOrderBefore) {
                            restoreHeaderOrderByIds(headerOrderBefore);
                            syncSwimlanesToHeader();
                        }
                        alert('Erreur serveur : impossible de réordonner les colonnes.');
                    });

                return false;
            });
        });
    }

    // ------------------------------------------------------------
    // 2) DRAG STORIES : WIP logique (double) + 2 compteurs + rollback
    // ------------------------------------------------------------

    let draggedCard = null;
    let sourceInfo = null; // {zone,parentId,nextSibling,rowKey,colKey}

    const cards = document.querySelectorAll('.issue-draggable');
    const dropZones = document.querySelectorAll('.droppable');

    function countLogicalCards(parentId) {
        if (!parentId) return 0;
        const zones = document.querySelectorAll(`.droppable[data-parent-id='${parentId}']`);
        let total = 0;
        zones.forEach(z => { total += z.querySelectorAll('.kb-card').length; });
        return total;
    }

    function updateCountersUI(parentId) {
        const counterSpan = document.getElementById('counter-span-' + parentId);
        if (!counterSpan) return;
        const total = countLogicalCards(parentId);
        const limit = counterSpan.getAttribute('data-limit');
        counterSpan.innerText = ` ${total} / ${limit}`;
    }

    function placeCard(zone, card, colKey) {
        if (!zone || !card) return;
        if (colKey === 'backlog' || colKey === 'closed') zone.prepend(card);
        else zone.appendChild(card);
    }

    function revertCard() {
        if (!draggedCard || !sourceInfo || !sourceInfo.zone) return;

        if (sourceInfo.nextSibling && sourceInfo.nextSibling.parentNode === sourceInfo.zone) {
            sourceInfo.zone.insertBefore(draggedCard, sourceInfo.nextSibling);
        } else {
            placeCard(sourceInfo.zone, draggedCard, sourceInfo.colKey);
        }
    }

    function saveMove(issueId, row, col) {
        return postFormUrlEncoded('/board/move-issue-dnd', {
            issueId: String(issueId),
            targetRowKey: String(row),
            targetColumnKey: String(col)
        }).then(async (resp) => {
            let data = null;
            try { data = await resp.json(); } catch { /* ignore */ }

            if (!resp.ok || (data && data.success === false)) {
                const msg = (data && data.message) ? data.message : 'Impossible de déplacer';
                throw new Error(msg);
            }
            return true;
        });
    }

    cards.forEach(card => {
        card.addEventListener('dragstart', function (e) {
            draggedCard = this;
            e.dataTransfer.effectAllowed = 'move';

            const parentZone = this.closest('.droppable');
            sourceInfo = {
                zone: parentZone,
                parentId: parentZone ? parentZone.getAttribute('data-parent-id') : null,
                nextSibling: this.nextElementSibling,
                rowKey: parentZone ? parentZone.getAttribute('data-row') : null,
                colKey: parentZone ? parentZone.getAttribute('data-col') : null
            };

            setTimeout(() => { this.style.display = 'none'; }, 0);
        });

        card.addEventListener('dragend', function () {
            setTimeout(() => { this.style.display = 'block'; }, 0);
            draggedCard = null;
            sourceInfo = null;
        });
    });

    dropZones.forEach(zone => {
        zone.addEventListener('dragover', function (e) {
            if (!draggedCard) return;
            e.preventDefault();
        });

        zone.addEventListener('dragenter', function () {
            if (!draggedCard) return;
            this.style.backgroundColor = 'rgba(0,0,0,0.05)';
        });

        zone.addEventListener('dragleave', function () {
            this.style.backgroundColor = '';
        });

        zone.addEventListener('drop', function (e) {
            if (!draggedCard) return;

            e.preventDefault();
            this.style.backgroundColor = '';

            const targetParentId = this.getAttribute('data-parent-id');
            const limit = parseInt(this.getAttribute('data-limit') || '0', 10);

            // WIP check logique : colonne double = parentId identique
            if (!isNaN(limit) && limit > 0 && targetParentId) {
                const total = countLogicalCards(targetParentId);
                const alreadyInLogical = sourceInfo && sourceInfo.parentId && sourceInfo.parentId === targetParentId;
                const afterMove = total + (alreadyInLogical ? 0 : 1);
                if (afterMove > limit) {
                    alert('⚠ Le nombre maximum de tâches pour cette colonne est atteint !');
                    return;
                }
            }

            const issueId = draggedCard.getAttribute('data-issue-id');
            const newRow = this.getAttribute('data-row');
            const newCol = this.getAttribute('data-col');

            // move optimiste
            placeCard(this, draggedCard, newCol);

            // maj compteurs : source + target
            if (sourceInfo && sourceInfo.parentId) updateCountersUI(sourceInfo.parentId);
            if (targetParentId) updateCountersUI(targetParentId);

            // persist serveur + rollback sans refresh
            saveMove(issueId, newRow, newCol).catch(err => {
                alert('Erreur serveur : ' + (err && err.message ? err.message : 'Impossible de déplacer'));
                revertCard();
                if (sourceInfo && sourceInfo.parentId) updateCountersUI(sourceInfo.parentId);
                if (targetParentId) updateCountersUI(targetParentId);
            });
        });
    });
});
