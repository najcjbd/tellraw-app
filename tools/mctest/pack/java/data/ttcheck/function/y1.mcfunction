# [Y1] 谓词里 damage=3 应命中损耗3的剑 | 期望匹配
scoreboard objectives add tt dummy
clear @s
item replace entity @s weapon.mainhand with minecraft:diamond_sword[minecraft:damage=3] 1
execute if items entity @s weapon.mainhand minecraft:diamond_sword[damage=3] run scoreboard players set #y1s tt 1
execute unless items entity @s weapon.mainhand minecraft:diamond_sword[damage=3] run scoreboard players set #y1s tt 0
execute if items entity @s weapon.mainhand minecraft:diamond_sword[damage=3] run scoreboard players set #y1 tt 1
execute unless items entity @s weapon.mainhand minecraft:diamond_sword[damage=3] run scoreboard players set #y1 tt 0
