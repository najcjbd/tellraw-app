# [Z8] Unbreakable:1b -> unbreakable:{} 对不对 | 期望匹配
scoreboard objectives add tt dummy
clear @s
item replace entity @s weapon.mainhand with minecraft:diamond_sword[minecraft:unbreakable={}] 1
execute if items entity @s weapon.mainhand minecraft:diamond_sword run scoreboard players set #z8s tt 1
execute unless items entity @s weapon.mainhand minecraft:diamond_sword run scoreboard players set #z8s tt 0
execute if items entity @s weapon.mainhand minecraft:diamond_sword[unbreakable={}] run scoreboard players set #z8 tt 1
execute unless items entity @s weapon.mainhand minecraft:diamond_sword[unbreakable={}] run scoreboard players set #z8 tt 0
