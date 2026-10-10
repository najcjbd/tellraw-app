# [C4] 玩家主手 equipment.mainhand 是否成立 | 只看实测值
scoreboard objectives add tt dummy
clear @s
item replace entity @s weapon.mainhand with minecraft:diamond_sword 1
execute if items entity @s weapon.mainhand minecraft:diamond_sword run scoreboard players set #c4s tt 1
execute unless items entity @s weapon.mainhand minecraft:diamond_sword run scoreboard players set #c4s tt 0
execute if data entity @s {equipment:{mainhand:{id:"minecraft:diamond_sword"}}} run scoreboard players set #c4 tt 1
execute unless data entity @s {equipment:{mainhand:{id:"minecraft:diamond_sword"}}} run scoreboard players set #c4 tt 0
