# [Z13] can_break 必须写成 {blocks:[…]}（z12/z13 实测确认） | 期望匹配
scoreboard objectives add tt dummy
clear @s
item replace entity @s weapon.mainhand with minecraft:diamond_pickaxe[minecraft:can_break={blocks:["minecraft:stone"]}] 1
execute if items entity @s weapon.mainhand minecraft:diamond_pickaxe run scoreboard players set #z13s tt 1
execute unless items entity @s weapon.mainhand minecraft:diamond_pickaxe run scoreboard players set #z13s tt 0
execute if items entity @s weapon.mainhand minecraft:diamond_pickaxe[can_break={blocks:["minecraft:stone"]}] run scoreboard players set #z13 tt 1
execute unless items entity @s weapon.mainhand minecraft:diamond_pickaxe[can_break={blocks:["minecraft:stone"]}] run scoreboard players set #z13 tt 0
