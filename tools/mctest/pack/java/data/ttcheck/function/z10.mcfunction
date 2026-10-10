# [Z10] display.color -> dyed_color 的形状 | 期望匹配
scoreboard objectives add tt dummy
clear @s
item replace entity @s armor.chest with minecraft:leather_chestplate[minecraft:dyed_color=16711680] 1
execute if items entity @s armor.chest minecraft:leather_chestplate run scoreboard players set #z10s tt 1
execute unless items entity @s armor.chest minecraft:leather_chestplate run scoreboard players set #z10s tt 0
execute if items entity @s armor.chest minecraft:leather_chestplate[dyed_color=16711680] run scoreboard players set #z10 tt 1
execute unless items entity @s armor.chest minecraft:leather_chestplate[dyed_color=16711680] run scoreboard players set #z10 tt 0
