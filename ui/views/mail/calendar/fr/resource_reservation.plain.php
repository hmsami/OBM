Message automatique envoyé par OBM
------------------------------------------------------------------
NOUVELLE RESERVATION DE RESSOURCE !
------------------------------------------------------------------

Une ressource dont vous êtes responsable à été réservée

du     : <?php echo $start; ?>

au     : <?php echo $end; ?>

sujet  : <?php echo $title; ?>

lieu   : <?php echo $location; ?>

auteur : <?php echo $auteur; ?>


:: Pour accepter/refuser la réservation : 
<?php echo $this->host; ?>/calendar/calendar_index.php?action=detailconsult&calendar_id=<?php echo $id; ?>
