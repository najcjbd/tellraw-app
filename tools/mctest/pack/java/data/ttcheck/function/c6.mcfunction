# [C6] 头盔 key equipment.head | 期望匹配
scoreboard objectives add tt dummy
clear @s
item replace entity @s armor.head with minecraft:diamond_helmet 1
execute if items entity @s armor.head minecraft:diamond_helmet run scoreboard players set #c6s tt 1
execute unless items entity @s armor.head minecraft:diamond_helmet run scoreboard players set #c6s tt 0
execute if data entity @s {equipment:{head:{id:"minecraft:diamond_helmet"}}} run scoreboard players set #c6 tt 1
execute unless data entity @s {equipment:{head:{id:"minecraft:diamond_helmet"}}} run scoreboard players set #c6 tt 0
