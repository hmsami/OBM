#!/usr/bin/perl -w -T
#####################################################################
# OBM               - File : updateSieve.pl                         #
#                   - Desc : Script permettant de gérer le filtre   #
#                   sieve d'un utilisateur                          #
#                   - Paramètres :                                  #
#                       uid : login de l'utilisateur à traiter      #
#                       deomain : domain ID de l'utilisateur        #
#####################################################################
# Retour :                                                          #
#    - 0 : tout c'est bien passé                                    #
#    - 1 : erreur à l'analyse des paramètres du script              #
#    - 2 : erruer à l'ouverture de la BD                            #
#####################################################################

use strict;
require OBM::toolBox;
require OBM::dbUtils;
require OBM::Update::updateSieve;
use OBM::Parameters::common;
use Getopt::Long;

$ENV{PATH}=$automateOBM;
delete @ENV{qw(IFS CDPATH ENV BASH_ENV PATH)};


# Fonction de vérification des paramètres du script
sub getParameter {
    my( $parameters ) = @_;

    # Analyse de la ligne de commande
    &GetOptions( $parameters, "login=s", "domain=s" );

    # Verification de l'identifiant utilisateur
    if( !exists($$parameters{"login"}) ) {
        &OBM::toolBox::write_log( "Parametre --login manquant", "W" );
        return 1;

    }else {
        if( $$parameters{"login"} !~ /$regexp_login/ ) {
            &OBM::toolBox::write_log( "Parametre --login invalide", "W" );
            return 1;
        }
    }

    # Verification du domaine
    if( !exists($$parameters{"domain"}) ) {
        &OBM::toolBox::write_log( "Parametre --domain manquant", "W" );
        return 1;
    }else {
        if( $$parameters{"domain"} !~ /^[0-9]+$/ ) {
            &OBM::toolBox::write_log( "Parametre --domain invalide", "W" );
            return 1;
        }
    }

    return 0;
}


# On prepare le log
&OBM::toolBox::write_log( "UpdateSieve: ", "O" );

# Traitement des parametres
&OBM::toolBox::write_log( "Analyse des parametres du script", "W" );
my %parameters;
if( getParameter( \%parameters ) ) {
    &OBM::toolBox::write_log( "", "C" );
    exit 1;
}

# On se connecte a la base
my $dbHandler;
&OBM::toolBox::write_log( "Connexion a la base de donnees OBM", "W" );
if( !&OBM::dbUtils::dbState( "connect", \$dbHandler ) ) {
    if( defined($dbHandler) ) {
        &OBM::toolBox::write_log( "Probleme lors de l'ouverture de la base de donnee : ".$dbHandler->err, "WC" );
    }else {
        &OBM::toolBox::write_log( "Probleme lors de l'ouverture de la base de donnee : erreur inconnue", "WC" );
    }

    exit 2;
}


my $errorCode = 0;
my $updateSieve = OBM::Update::updateSieve->new( $dbHandler, \%parameters );
if( defined($updateSieve) ) {
    $errorCode = $updateSieve->update();
}


# On referme la connexion à la base
if( !&OBM::dbUtils::dbState( "disconnect", \$dbHandler ) ) {
    &OBM::toolBox::write_log( "Probleme lors de la fermeture de la base de donnees...", "W" );
}


# On ferme le log
&OBM::toolBox::write_log( "Execution du script terminee", "WC", 0 );

exit !$errorCode;
