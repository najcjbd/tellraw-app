# [B19] 含0区间：空槽实测(第六组说不命中) | 期望不匹配
scoreboard objectives add tt dummy
clear @s
execute if items entity @s hotbar.5 minecraft:diamond[count~{min:0,max:4}] run scoreboard players set #b19 tt 1
execute unless items entity @s hotbar.5 minecraft:diamond[count~{min:0,max:4}] run scoreboard players set #b19 tt 0
