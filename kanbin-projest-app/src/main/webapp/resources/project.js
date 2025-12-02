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
});