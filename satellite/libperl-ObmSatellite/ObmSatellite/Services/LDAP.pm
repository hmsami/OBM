package ObmSatellite::Services::LDAP;

$VERSION = '1.0';

$debug = 1;

use Class::Singleton;
use ObmSatellite::Log::log;
@ISA = qw(Class::Singleton ObmSatellite::Log::log);

use 5.006_001;
require Exporter;
use strict;

require Config::IniFiles;

use constant OBM_CONF => '/etc/obm/obm_conf.ini';
# LDAP server dead time (s)
use constant DEAD_STATUS_TIME => 180;


sub _new_instance {
    my $class = shift;
    my( $confFile ) = @_;

    my $self = bless { }, $class;

    my $ldapDesc = $self->_loadObmConf();
    my $ldapDescConfFile = $self->_loadConfFile( $confFile );

    if( !defined($ldapDesc) && !defined($ldapDescConfFile) ) {
        $self->_log( 'No LDAP server configuration', 1 );
        return undef;
    }elsif( defined($ldapDescConfFile) ) {
        while( my( $option, $value ) = each(%{$ldapDescConfFile}) ) {
            $ldapDesc->{$option} = $value;
        }
    }


    my $regexp_ip = '^((ldap|ldaps):\/\/){0,1}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)[/]{0,1}$';
    my $regexp_hostname = '^((ldap|ldaps):\/\/){0,1}[a-z0-9-]+(\.[a-z0-9-]+)*(\.[a-z]{2,6}){0,1}[/]{0,1}$';

    # Load LDAP configuration options
    if( defined($ldapDesc->{'ldap_server'}) && (($ldapDesc->{'ldap_server'} =~ $regexp_ip || ($ldapDesc->{'ldap_server'} =~ /$regexp_hostname/)) ) ) {
        my %ldapDesc;

        $self->{'ldap_server'} = $ldapDesc->{'ldap_server'};
        if( $self->{'ldap_server'} !~ /^(ldap|ldaps):/ ) {
            $self->{'ldap_server'} = 'ldap://'.$self->{'ldap_server'}.'/';
        }

        $self->{'ldap_server_tls'} = $ldapDesc->{'ldap_server_tls'};
        if( $self->{'ldap_server'} =~ /^ldaps:/ ) {
            $self->{'ldap_server_tls'} = 'none';
        }
        if( !defined($self->{'ldap_server_tls'}) || ($self->{'ldap_server_tls'} !~ /^(none|may|encrypt)$/) ) {
            $self->{'ldap_server_tls'} = 'may';
        }

        $self->{'ldap_login'} = $ldapDesc->{'ldap_login'};
        $self->{'ldap_password'} = $ldapDesc->{'ldap_password'};
        $self->{'ldap_root'} = $ldapDesc->{'ldap_root'};

    }else {
        $self->_log( 'ldap_server not defined or incorrect in configuration file', 1 );
        return undef;
    }

    return $self;
}


sub getDescription {
    my $self = shift;

    return 'LDAP server \''.$self->{'ldap_server'}.'\'';
}


sub _loadObmConf {
    my $self = shift;
    my %ldapDesc;

    if( !(-f OBM_CONF && -r OBM_CONF) ) {
        return undef;
    }

    my $cfgFile = Config::IniFiles->new( -file => OBM_CONF );
    return undef if !defined($cfgFile);

    my $iniValue = $cfgFile->val( 'automate', 'ldapServer' );
    $ldapDesc{'ldap_server'} = $iniValue if $iniValue;

    $iniValue = $cfgFile->val( 'automate', 'ldapTls' );
    $ldapDesc{'ldap_server_tls'} = $iniValue if $iniValue;

    $iniValue = $cfgFile->val( 'automate', 'ldapRoot' );
    if( $iniValue ) {
        my @root = split( /,/, $iniValue );
        $iniValue = '';
        while( my $part = pop(@root) ) {
            if( $iniValue ) {
                $iniValue = ','.$iniValue;
            }

            $iniValue = 'dc='.$part.$iniValue;
        }
    }
    $ldapDesc{'ldap_root'} = $iniValue if $iniValue;

    return \%ldapDesc;
}


sub _loadConfFile {
    my $self = shift;
    my( $confFile ) = @_;
    my %ldapDesc;

    if( !(-f $confFile && -r $confFile) ) {
        return undef;
    }

    my $cfgFile = Config::IniFiles->new( -file => $confFile );
    return undef if !defined($cfgFile);

    my $iniValue = $cfgFile->val( 'server', 'ldapServer' );
    $ldapDesc{'ldap_server'} = $iniValue if $iniValue;

    $iniValue = $cfgFile->val( 'server', 'ldapTls' );
    $ldapDesc{'ldap_server_tls'} = $iniValue if $iniValue;

    $iniValue = $cfgFile->val( 'server', 'ldapRoot' );
    $ldapDesc{'ldap_root'} = $iniValue if $iniValue;

    $iniValue = $cfgFile->val( 'server', 'ldapLogin' );
    $ldapDesc{'ldap_login'} = $iniValue if $iniValue;

    $iniValue = $cfgFile->val( 'server', 'ldapPassword' );
    $ldapDesc{'ldap_password'} = $iniValue if $iniValue;

    return \%ldapDesc;
}


sub DESTROY {
    my $self = shift;


    $self->_log( 'Deleting LDAP server', 5 );

    if( ref( $self->{'ldapServerConn'} ) eq 'Net::LDAP' ) {
        $self->disconnect();
    }else {
        undef $self->{'ldapServerConn'};
    }
}


sub disconnect {
    my $self = shift;

    if( ref( $self->{'ldapServerConn'} ) ne 'Net::LDAP' ) {
        $self->_log( 'LDAP not connected', 4 );

        $self->{'ldapServerConn'} = undef;
        return 0;
    }

    if( $self->_ping($self->{'ldapServerConn'}) ) {
        $self->_log( 'Disconnect from LDAP server', 4 );
        # Trying to unbind silently...
        eval{ $self->{'ldapServerConn'}->disconnect() };

        $self->{'ldapServerConn'} = undef;
        return 0;
    }

    $self->_log( 'LDAP already disconnected', 4 );

    $self->{'ldapServerConn'} = undef;
    return 0;
}


sub getConn {
    my $self = shift;

    if( ref( $self->{'ldapServerConn'} ) ne 'Net::LDAP' ) {
        if( $self->{'ldapServerConn'} = $self->_connect() ) {
            $self->_searchAuthenticate();
        }
    }elsif( !$self->_ping($self->{'ldapServerConn'}) ) {
        $self->_log( 'LDAP server ping failed. Try to reconnect...', 4 );
        $self->{'ldapServerConn'} = undef;
        return $self->getConn();
    }else {
        $self->_log( 'LDAP server connection already established', 4 );
    }

    return $self->{'ldapServerConn'};
}


sub _connect {
    my $self = shift;
    my $ldapServerConn;


    if( $self->getDeadStatus() ) {
        $self->_log( 'LDAP server is disable', 1 );
        return undef;
    }

    $self->_log( 'connect to '.$self->getDescription().' '.$self->{'ldap_server'}.'...', 4 );

    my @tempo = ( 1, 3, 5, 10, 20, 30 );
    require Net::LDAP;
    while( !($ldapServerConn = Net::LDAP->new( $self->{'ldap_server'}, debug => '0', timeout => '60', version => '3' )) ) {
        my $tempo = shift(@tempo);
        if( !defined($tempo) ) {
            last;
        }

        $self->_log( 'LDAP connection failed. Retry in '.$tempo.'s', 2 );
        sleep $tempo;
    }

    if( !$ldapServerConn ) {
        $self->_log( 'Can\'t connect to '.$self->getDescription().'. Disabling LDAP server', 1 );
        $self->_setDeadStatus();
        return undef;
    }


    use Net::LDAP qw(LDAP_EXTENSION_START_TLS);
    my $ldapDse = $ldapServerConn->root_dse();
    if( !defined($ldapDse) ) {
        $self->_log( 'Can\'t get LDAP root DSE. Can\'t check for TLS/SSL server support. Check server ACLs', 1 );
    }elsif( !$ldapDse->supported_extension(LDAP_EXTENSION_START_TLS) ) {
        $self->_log( 'no TLS/SSL LDAP server support', 1 );
        $self->{'ldap_server_tls'} = 'none';
    }

    use Net::LDAP qw(LDAP_CONFIDENTIALITY_REQUIRED);
    if( $self->{'ldap_server_tls'} =~ /^(may|encrypt)$/ ) {
        my $error = $ldapServerConn->start_tls( verify => 'none' );

        if( $error->code() && ($self->{'ldap_server_tls'} eq 'encrypt') ) {
            $self->_log( 'fatal error on start_tls : '.$error->error, 1 );
            $self->_log( 'TLS connection needed by configuration. Disabling LDAP server', 1 );
            $self->_setDeadStatus();
            return undef;
        }

        if( $error->code() && ($self->{'ldap_server_tls'} eq 'may') ) {
            $self->_log( 'fatal error on start_tls : '.$error->error, 1 );
            $self->_log( 'TLS not needed, trying to reconnect without TLS', 1 );
            $self->{'ldap_server_tls'} = 'none';

            $ldapServerConn = undef;
            return $self->_connect();
        }

        if( !$error->code() ) {
            $self->_log( 'TLS connection established', 4 );
        }
    }

    return $ldapServerConn;
}


# Must be connect to LDAP server before calling
sub _searchAuthenticate {
    my $self = shift;

    # LDAP authentication
    my $error; 
    if( $self->{'ldap_login'} ) {
        $self->_log( 'Authenticating to '.$self->getDescription().' as user DN '.$self->{'ldap_login'}, 3 );
        $error = $self->{'ldapServerConn'}->bind(
            $self->{'ldap_login'},
            password => $self->{'ldap_password'}
        );
    }else {
        $self->_log( 'Authenticating anonymously', 3 );
        $error = $self->{'ldapServerConn'}->bind();
    }

    if( !$error->code ) {
        $self->_log( 'LDAP connection success', 3 );

    }elsif( $error->code == LDAP_CONFIDENTIALITY_REQUIRED ) {
        $self->_log( 'start_tls needed by LDAP server. Check your configuration', 1 );
        $self->_log( 'disabling LDAP server', 1 );
        $self->_setDeadStatus();
        $self->{'ldapServerConn'} = undef;
        return 1;

    }elsif( $error->code ) {
        $self->_log( 'fail to authenticate against LDAP server : '.$error->error, 1 );
        $self->{'ldapServerConn'} = undef;
        return 1;
    }

    return 0;
}


sub checkAuthentication {
    my $self = shift;
    my( $dn, $passwd ) = @_;

    my $ldapConn = $self->getConn();

    # LDAP authentication
    my $error = $ldapConn->bind(
        $dn,
        password => $passwd
    );

    my $returnCode = 0;
    if( !$error->code ) {
        $self->_log( 'LDAP authentication success for user '.$dn, 3 );
        $returnCode = 1;
    
    }elsif( $error->code == LDAP_CONFIDENTIALITY_REQUIRED ) {
        $self->_log( 'start_tls needed by LDAP server. Check your configuration', 1 );
        $self->_log( 'disabling LDAP server', 1 );
        $self->_setDeadStatus();
        $self->{'ldapServerConn'} = undef;

    }else {
        $self->_log( 'LDAP authentication fail for user '.$dn, 1 );
    }

    $self->_searchAuthenticate();
    return $returnCode;
}



sub getDeadStatus {
    my $self = shift;

    return $self->{'deadStatus'} if !$self->{'deadStatus'};

    if( (time() - $self->{'deadStatus'}) < DEAD_STATUS_TIME ) {
        return 1;
    }

    return $self->_unsetDeadStatus();
}


sub _setDeadStatus {
    my $self = shift;

    $self->{'deadStatus'} = time();

    return 0;
}


sub _unsetDeadStatus {
    my $self = shift;

    $self->{'ldapServerConn'} = undef;
    $self->{'deadStatus'} = 0;

    return 0;
}


sub _ping {
    my $self = shift;
    my( $ldapServerConn ) = @_;

    if( !defined($ldapServerConn) ) {
        return 0;
    }

    my $result = $ldapServerConn->search(
                    scope => 'base',
                    filter => '(objectclass=*)',
                    sizelimit => 1
                    );

    if( $result->is_error() ) {
        return 0;
    }

    return 1;
}


sub getLdapRoot {
    my $self = shift;

    return $self->{'ldap_root'};
}
