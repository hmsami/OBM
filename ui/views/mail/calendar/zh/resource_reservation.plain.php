﻿<?php
/******************************************************************************
Copyright (C) 2011-2012 Linagora

This program is free software: you can redistribute it and/or modify it under
the terms of the GNU Affero General Public License as published by the Free
Software Foundation, either version 3 of the License, or (at your option) any
later version, provided you comply with the Additional Terms applicable for OBM
software by Linagora pursuant to Section 7 of the GNU Affero General Public
License, subsections (b), (c), and (e), pursuant to which you must notably (i)
retain the displaying by the interactive user interfaces of the “OBM, Free
Communication by Linagora” Logo with the “You are using the Open Source and
free version of OBM developed and supported by Linagora. Contribute to OBM R&D
by subscribing to an Enterprise offer !” infobox, (ii) retain all hypertext
links between OBM and obm.org, between Linagora and linagora.com, as well as
between the expression “Enterprise offer” and pro.obm.org, and (iii) refrain
from infringing Linagora intellectual property rights over its trademarks and
commercial brands. Other Additional Terms apply, see
<http://www.linagora.com/licenses/> for more details.

This program is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License and
its applicable Additional Terms for OBM along with this program. If not, see
<http://www.gnu.org/licenses/> for the GNU Affero General   Public License
version 3 and <http://www.linagora.com/licenses/> for the Additional Terms
applicable to the OBM software.
//?>
This email was automatically sent by OBM
------------------------------------------------------------------
NEW RESOURCE RESERVATION !
------------------------------------------------------------------

A new resource reservation was scheduled

Ressource name: <?php  echo $resourceLabel; ?>

from     : <?php echo $start; ?>

to           : <?php echo $end; ?>

subject      : <?php echo $title; ?>

location     : <?php echo $location; ?>

organizer    : <?php echo $organizer; ?>

created by   : <?php echo $creator; ?>

Comment : <?php echo $targetComment; ?>.

:: To accept or refuse this reservation :
<?php echo $this->host; ?>/calendar/calendar_index.php?action=detailconsult&calendar_id=<?php echo $id; ?>
******************************************************************************/


?>
此邮件由OBM自动发送
------------------------------------------------------------------
新资源预订！
------------------------------------------------------------------

一个新的资源预订计划已被制定

资源名称	: <?php  echo $resourceLabel; ?>

从     		: <?php echo $start; ?>

到          : <?php echo $end; ?>

主题        : <?php echo $title; ?>

地点        : <?php echo $location; ?>

组织者      : <?php echo $organizer; ?>

创建者    	: <?php echo $creator; ?>

评论 		: <?php echo $targetComment; ?>.

::要接受或拒绝此资源预订:
<?php echo $this->host; ?>/calendar/calendar_index.php?action=detailconsult&calendar_id=<?php echo $id; ?>