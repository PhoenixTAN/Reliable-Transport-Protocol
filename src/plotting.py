import matplotlib.pyplot as plt


x = []

for i in range(0,10):
    x.append(i*0.05)

print(x)

# loss = [18.011, 22.493, 27.877, 34.4, 41.908, 54.211, 61.877, 83.117, 97.060, 133.315]
# plt.plot(x, loss)
# plt.title('Average communication time as a function loss')
# plt.xlabel('loss probability')
# plt.ylabel('Average communication time')
# plt.show()

# corruption = [19.797, 22.408, 27.877, 32.540, 35.584, 48.189, 66.138, 75.535, 91.262, 132.136]
# plt.plot(x, corruption)
# plt.title('Average communication time as a function corruption')
# plt.xlabel('corruption probability')
# plt.ylabel('Average communication time')
# plt.show()

# retransmit_vs_loss = [25, 39, 58, 79, 103, 135, 159, 209, 247, 318]
# plt.plot(x, retransmit_vs_loss)
# plt.title('Retransmits as function of loss')
# plt.xlabel('loss probability')
# plt.ylabel('Retransmission')
# plt.show()

# RTT_vs_loss = [12.592, 16.161, 16.420, 20.009, 21.760, 29.737, 35.490, 32.251, 39.772, 51.128]
# plt.plot(x, RTT_vs_loss)
# plt.title('Average RTT as function of loss')
# plt.xlabel('loss probability')
# plt.ylabel('Average RTT')
# plt.show()

# retransmit_vs_corrut = [30, 39, 58, 70, 84, 120, 165, 191, 227, 318]
# plt.plot(x, retransmit_vs_corrut)
# plt.title('Retransmits as function of corruption')
# plt.xlabel('Corruption Probability')
# plt.ylabel('Retransmits')
# plt.show()

RTT_vs_corrut = [12.546, 13.085, 16.420, 17.798, 20.989, 23.700, 32.216, 41.050, 38.063, 64.283]
plt.plot(x, RTT_vs_corrut)
plt.title('Average RTT as function of corruption')
plt.xlabel('Corruption Probability')
plt.ylabel('Average RTT')
plt.show()
