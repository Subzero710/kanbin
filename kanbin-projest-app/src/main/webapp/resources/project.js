document.addEventListener("DOMContentLoaded", function() {
    const columns = document.querySelectorAll('.kb-col-draggable');
    const board = document.getElementById('board');
    let draggedItem = null;

    columns.forEach(col => {
        // Restriction du drag à l'en-tête

        // VARIABLE D'ÉTAT : On stocke ici si le clic était valide
        let isCursorInHeader = false;

        // 1. ÉTAPE DE DÉTECTION (Avant le drag)
        col.addEventListener('mousedown', function(e) {
            // On regarde si l'élément cliqué est dans le header
            if (e.target.closest('.kb-col-header')) {
                isCursorInHeader = true;
            } else {
                isCursorInHeader = false;
            }
        });

        // 2. DÉMARRAGE DU DRAG
        col.addEventListener('dragstart', function(e) {
            // Si le clic initial n'était pas dans le header, on coupe tout
            if (!isCursorInHeader) {
                e.preventDefault();
                return;
            }

            draggedItem = this;
            setTimeout(() => this.style.opacity = '0.4', 0);
            e.dataTransfer.effectAllowed = 'move';
        });

        // Fin du drag (nettoyage)
        col.addEventListener('dragend', function() {
            this.style.opacity = '1';
            draggedItem = null;
            // Retirer les indicateurs visuels s'il y en a
            document.querySelectorAll('.kb-col').forEach(c => c.style.border = "");
        });

        // Au survol d'une zone de dépôt
        col.addEventListener('dragover', function(e) {
            e.preventDefault(); // Nécessaire pour autoriser le drop
        });

        col.addEventListener('dragenter', function(e) {
            e.preventDefault();
            // On ne met une bordure que si ce n'est pas l'élément qu'on traine
            if (this !== draggedItem) {
                this.style.border = "4px dashed #666";
            }
        });

        col.addEventListener('dragleave', function() {
            this.style.border = "";
        });

        // Le relachement (DROP)
        col.addEventListener('drop', function(e) {
            this.style.border = "";
            e.preventDefault();

            // Sécurité : ne rien faire si on drop sur soi-même
            if (this === draggedItem) return;

            // Logique d'insertion dans le DOM
            // On insère "draggedItem" devant ou après "this" (la cible)
            const mainCols = Array.from(board.querySelectorAll('.kb-main-col'));

            const draggedIndex = mainCols.indexOf(draggedItem);
            const targetIndex = mainCols.indexOf(this);

            if (draggedIndex === -1 || targetIndex === -1) {
                console.error("Impossible de trouver l'index : élément non reconnu comme colonne principale.");
                return;
            }

            // Interdit de toucher à l'index 0 (La première colonne fixe)
            if (targetIndex === 0) {
                alert("Impossible de placer une colonne avant la colonne de départ.");
                return;
            }

            // Déplacement visuel (DOM)
            if (draggedIndex < targetIndex) {
                this.after(draggedItem);
            } else {
                this.before(draggedItem);
            }

            // Sauvegarde AJAX
            saveNewOrder(draggedItem, draggedItem.getAttribute('data-id'), targetIndex);
        });
    });

    function saveNewOrder(element, columnId, newIndex) {
        // On construit les données du formulaire
        const formData = new URLSearchParams();
        formData.append('columnId', columnId);
        formData.append('newIndex', newIndex);

        fetch('/board/reorder-column', {
            method: 'POST',
            body: formData,
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            }
        })
        .then(response => response.text())
        .then(data => {
            if (data === "OK") {
                // SUCCÈS : On ajoute la classe verte
                element.classList.add('flash-success');
                // On la retire après l'animation pour pouvoir la rejouer plus tard
                setTimeout(() => element.classList.remove('flash-success'), 1500);
            } else {
                // ERREUR SERVEUR
                console.error("Erreur:", data);
                handleError(element);
            }
        })
        .catch(err => {
            console.error(err);
            handleError(element);
        });
    }

    function handleError(element) {
        // En cas d'erreur, on secoue l'élément en rouge
        element.classList.add('flash-error');
        setTimeout(() => {
            element.classList.remove('flash-error');
            alert("Erreur lors de la sauvegarde du déplacement. La page va être rechargée.");
            location.reload(); // On recharge pour remettre l'ordre correct
        }, 500);
    }

    // ============================================================
    // GESTION DU DRAG & DROP DES STORIES (ISSUES)
    // ============================================================

    const draggableIssues = document.querySelectorAll('.issue-draggable');
    let draggedIssue = null;

    // 1. Début du drag sur une story
    draggableIssues.forEach(issue => {
        issue.addEventListener('dragstart', function(e) {
            draggedIssue = this;
            e.dataTransfer.effectAllowed = 'move';
            e.dataTransfer.setData('text/plain', this.getAttribute('data-id'));
            setTimeout(() => this.style.opacity = '0.5', 0);
            e.stopPropagation(); // Empêche la colonne de bouger
        });

        issue.addEventListener('dragend', function() {
            this.style.opacity = '1';
            draggedIssue = null;
            document.querySelectorAll('.kb-col-body').forEach(b => b.style.background = "");
        });
    });

    // 2. Zone de dépôt (Corps des colonnes)
    const issueDropZones = document.querySelectorAll('.kb-col-body');

    issueDropZones.forEach(zone => {
        zone.addEventListener('dragover', function(e) {
            e.preventDefault();
        });

        zone.addEventListener('dragenter', function(e) {
            e.preventDefault();
            if (draggedIssue) this.style.background = "#eef0f3";
        });

        zone.addEventListener('dragleave', function() {
            this.style.background = "";
        });

        zone.addEventListener('drop', function(e) {
            this.style.background = "";
            if (!draggedIssue) return; // Sécurité

            e.preventDefault();
            e.stopPropagation();

            const targetCol = this.closest('[data-col]');
            if (!targetCol) return;

            // Déplacement visuel
            this.appendChild(draggedIssue);

            // Sauvegarde AJAX
            const issueId = draggedIssue.getAttribute('data-id');
            const targetKey = targetCol.getAttribute('data-col');
            saveIssueMove(draggedIssue, issueId, targetKey);
        });
    });

    function saveIssueMove(element, issueId, targetColumnKey) {
        const formData = new URLSearchParams();
        formData.append('issueId', issueId);
        formData.append('targetColumnKey', targetColumnKey);

        fetch('/board/move-issue-dnd', {
            method: 'POST',
            body: formData,
            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
        })
            .then(response => response.text())
            .then(data => {
                if (data === "OK") {
                    element.classList.add('flash-success');
                    setTimeout(() => element.classList.remove('flash-success'), 1500);
                } else {
                    element.classList.add('flash-error');
                    console.error("Erreur:", data);
                }
            });
    }
});