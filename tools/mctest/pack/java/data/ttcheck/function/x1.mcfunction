# [X1] 胸甲 key equipment.chest | 期望匹配
scoreboard objectives add tt dummy
clear @s
item replace entity @s armor.chest with minecraft:diamond_chestplate 1
execute if items entity @s armor.chest minecraft:diamond_chestplate run scoreboard players set #x1s tt 1
execute unless items entity @s armor.chest minecraft:diamond_chestplate run scoreboard players set #x1s tt 0
execute if data entity @s {equipment:{chest:{id:"minecraft:diamond_chestplate"}}} run scoreboard players set #x1 tt 1
execute unless data entity @s {equipment:{chest:{id:"minecraft:diamond_chestplate"}}} run scoreboard players set #x1 tt 0
