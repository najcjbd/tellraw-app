# [Z11] RepairCost -> repair_cost 的形状 | 期望匹配
scoreboard objectives add tt dummy
clear @s
item replace entity @s weapon.mainhand with minecraft:diamond_pickaxe[minecraft:repair_cost=5] 1
execute if items entity @s weapon.mainhand minecraft:diamond_pickaxe run scoreboard players set #z11s tt 1
execute unless items entity @s weapon.mainhand minecraft:diamond_pickaxe run scoreboard players set #z11s tt 0
execute if items entity @s weapon.mainhand minecraft:diamond_pickaxe[repair_cost=5] run scoreboard players set #z11 tt 1
execute unless items entity @s weapon.mainhand minecraft:diamond_pickaxe[repair_cost=5] run scoreboard players set #z11 tt 0
