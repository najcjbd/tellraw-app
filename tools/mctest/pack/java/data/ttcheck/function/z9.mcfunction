# [Z9] 普通剑不应匹配 unbreakable | 期望不匹配
scoreboard objectives add tt dummy
clear @s
item replace entity @s weapon.mainhand with minecraft:diamond_sword[minecraft:damage=3] 1
execute if items entity @s weapon.mainhand minecraft:diamond_sword[damage=3] run scoreboard players set #z9s tt 1
execute unless items entity @s weapon.mainhand minecraft:diamond_sword[damage=3] run scoreboard players set #z9s tt 0
execute if items entity @s weapon.mainhand minecraft:diamond_sword[unbreakable={}] run scoreboard players set #z9 tt 1
execute unless items entity @s weapon.mainhand minecraft:diamond_sword[unbreakable={}] run scoreboard players set #z9 tt 0
