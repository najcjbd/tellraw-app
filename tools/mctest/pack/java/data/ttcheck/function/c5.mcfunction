# [C5] 副手 key 是否 equipment.offhand | 只看实测值
scoreboard objectives add tt dummy
clear @s
item replace entity @s weapon.offhand with minecraft:diamond_sword 1
execute if items entity @s weapon.offhand minecraft:diamond_sword run scoreboard players set #c5s tt 1
execute unless items entity @s weapon.offhand minecraft:diamond_sword run scoreboard players set #c5s tt 0
execute if data entity @s {equipment:{offhand:{id:"minecraft:diamond_sword"}}} run scoreboard players set #c5 tt 1
execute unless data entity @s {equipment:{offhand:{id:"minecraft:diamond_sword"}}} run scoreboard players set #c5 tt 0
