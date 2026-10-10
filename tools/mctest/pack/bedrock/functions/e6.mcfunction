# [E6] 背包第0格（Bedrock slot.inventory 0） | 期望匹配
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.inventory 0 diamond 1
execute if entity @s[hasitem={item=diamond,location=slot.inventory,slot=0}] run scoreboard players set #e6s tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.inventory,slot=0}] run scoreboard players set #e6s tt 0
execute if entity @s[hasitem={item=diamond,location=slot.inventory,slot=0}] run scoreboard players set #e6 tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.inventory,slot=0}] run scoreboard players set #e6 tt 0
