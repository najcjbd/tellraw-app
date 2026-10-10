# [B2] inventory.* 是否存在/覆盖背包 | 只看实测值
scoreboard objectives add tt dummy
clear @s
item replace entity @s inventory.0 with minecraft:diamond 1
execute if items entity @s inventory.0 minecraft:diamond run scoreboard players set #b2s tt 1
execute unless items entity @s inventory.0 minecraft:diamond run scoreboard players set #b2s tt 0
execute if items entity @s inventory.* minecraft:diamond run scoreboard players set #b2 tt 1
execute unless items entity @s inventory.* minecraft:diamond run scoreboard players set #b2 tt 0
