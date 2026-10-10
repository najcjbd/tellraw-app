# [Z6] 末影箱能否用 | 只看实测值
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.enderchest 0 diamond 1
execute if entity @s[hasitem={item=diamond,location=slot.enderchest}] run scoreboard players set #z6s tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.enderchest}] run scoreboard players set #z6s tt 0
execute if entity @s[hasitem={item=diamond,location=slot.enderchest}] run scoreboard players set #z6 tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.enderchest}] run scoreboard players set #z6 tt 0
