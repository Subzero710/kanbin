document.addEventListener("DOMContentLoaded", function() {
    console.log("Kanbin Project JS Loaded");

    // --- 1. DRAG COLONNES (Inchangé) ---
    let draggedColumn = null;
    const columns = document.querySelectorAll('.col-header-item[draggable="true"]');

    columns.forEach(col => {
        col.addEventListener('dragstart', function(e) { draggedColumn = this; e.dataTransfer.effectAllowed = 'move'; this.style.opacity = '0.4'; });
        col.addEventListener('dragend', function() { this.style.opacity = '1'; draggedColumn = null; columns.forEach(c => c.style.border = ""); });
        col.addEventListener('dragover', function(e) { e.preventDefault(); return false; });
        col.addEventListener('dragenter', function() { if(this !== draggedColumn) this.style.border = "2px dashed #666"; });
        col.addEventListener('dragleave', function() { this.style.border = ""; });
        col.addEventListener('drop', function(e) {
            e.stopPropagation(); this.style.border = "";
            if (draggedColumn && draggedColumn !== this) {
                let parent = this.parentNode;
                const allCols = Array.from(parent.children);
                const indexDragged = allCols.indexOf(draggedColumn);
                const indexTarget = allCols.indexOf(this);
                if (indexDragged < indexTarget) parent.insertBefore(draggedColumn, this.nextSibling);
                else parent.insertBefore(draggedColumn, this);
            }
            return false;
        });
    });

    // --- 2. DRAG STORIES + UPDATE COMPTEUR ---
    let draggedCard = null;
    let sourceParentId = null; // Pour se souvenir d'où vient la carte

    const cards = document.querySelectorAll('.issue-draggable');
    const dropZones = document.querySelectorAll('.droppable');

    cards.forEach(card => {
        card.addEventListener('dragstart', function(e) {
            draggedCard = this;
            e.dataTransfer.effectAllowed = 'move';
            // On mémorise la colonne d'origine pour mettre à jour son compteur plus tard
            let parentZone = this.closest('.droppable');
            if (parentZone) sourceParentId = parentZone.getAttribute('data-parent-id');

            setTimeout(() => this.style.display = 'none', 0);
        });
        card.addEventListener('dragend', function() {
            setTimeout(() => this.style.display = 'block', 0); draggedCard = null; sourceParentId = null;
        });
    });

    dropZones.forEach(zone => {
        zone.addEventListener('dragover', function(e) { e.preventDefault(); });
        zone.addEventListener('dragenter', function() { this.style.backgroundColor = 'rgba(0,0,0,0.05)'; });
        zone.addEventListener('dragleave', function() { this.style.backgroundColor = ''; });

        zone.addEventListener('drop', function(e) {
            e.preventDefault();
            this.style.backgroundColor = '';

            let limitAttr = this.getAttribute('data-limit');
            let limit = parseInt(limitAttr);
            // On compte les cartes DEJA présentes
            let currentCards = this.querySelectorAll('.kb-card').length;

            // Vérification Limite
            if (!isNaN(limit) && limit > 0 && currentCards >= limit) {
                alert("⚠Le nombre maximum de tâches pour cette colonne est atteint !");
                return;
            }

            if (draggedCard) {
                // 1. DÉPLACEMENT VISUEL SELON LA RÈGLE
                let newCol = this.getAttribute('data-col');

                // Si Backlog ou Closed -> On ajoute en HAUT (prepend)
                if (newCol === 'backlog' || newCol === 'closed') {
                    this.prepend(draggedCard);
                }
                // Sinon (Todo, Wip...) -> On ajoute en BAS (appendChild)
                else {
                    this.appendChild(draggedCard);
                }

                // 2. Mise à jour des compteurs (inchangé)
                let targetParentId = this.getAttribute('data-parent-id');
                if (targetParentId) updateCountersUI(targetParentId);
                let parentZone = draggedCard.closest('.droppable'); // Astuce pour récupérer l'ancien parent si besoin
                // (Note : ta logique précédente sourceParentId était meilleure si tu l'as gardée, sinon pas grave)

                // 3. Sauvegarde Serveur (inchangé)
                let issueId = draggedCard.getAttribute('data-issue-id');
                let newRow = this.getAttribute('data-row');
                // newCol est déjà défini au dessus
                saveMove(issueId, newRow, newCol);
            }
        });
    });

    // --- FONCTION DE MISE A JOUR COMPTEUR ---
    function updateCountersUI(parentId) {
        // 1. Trouver le span du compteur dans le header
        let counterSpan = document.getElementById('counter-span-' + parentId);
        if (!counterSpan) return; // Pas de limite sur cette colonne

        // 2. Trouver TOUTES les zones de drop associées à cet ID parent (pour gérer les colonnes doubles)
        let relatedZones = document.querySelectorAll(`.droppable[data-parent-id='${parentId}']`);

        // 3. Compter le total des cartes dans ces zones
        let totalCards = 0;
        relatedZones.forEach(z => {
            totalCards += z.querySelectorAll('.kb-card').length;
        });

        // 4. Mettre à jour le texte
        let limit = counterSpan.getAttribute('data-limit');
        counterSpan.innerText = ` ${totalCards} / ${limit}`;
    }

    function saveMove(issueId, row, col) {
        fetch('/board/move-issue-dnd', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: `issueId=${issueId}&targetRowKey=${row}&targetColumnKey=${col}`
        })
            .then(response => {
                if (!response.ok) {
                    return response.json().then(data => {
                        alert("Erreur serveur : " + (data.message || "Impossible de déplacer"));
                        window.location.reload();
                    }).catch(() => { window.location.reload(); });
                }
            })
            .catch(err => { console.error("Erreur AJAX:", err); window.location.reload(); });
    }
});